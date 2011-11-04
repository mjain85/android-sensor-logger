package at.jku.cp.feichtinger.sensorLogger.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import at.jku.cp.feichtinger.sensorLogger.ApplicationConstants;

public class RecorderService extends Service {
	private static final String TAG = "at.jku.cp.feichtinger.sensorLogger.services.RecorderService";

	private static boolean isRunning = false;

	private Map<Integer, BlockingQueue<SensorEvent>> data;
	private Map<Integer, Thread> consumers;
	private Map<Integer, Sensor> sensors;

	private final Binder mBinder = new RecorderBinder();
	private SensorManager sensorManager;
	private long startTime;
	private String activity;

	/**
	 * Listens for accelerometer events and stores them.
	 */
	private final SensorEventListener sensorEventListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(final SensorEvent event) {
			Log.i(TAG, "onSensorChanged called");
			final BlockingQueue<SensorEvent> dataQueue = data.get(event.sensor.getType());

			if (dataQueue != null) {
				try {
					dataQueue.put(event);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
			// TODO
		}
	};

	public static boolean isRunning() {
		return isRunning;
	}

	/* ***************************************************************
	 * service life-cycle methods
	 */

	/**
	 * Called by the system when the service is first created. Do not call this
	 * method directly.
	 */
	@Override
	public void onCreate() {
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		data = new HashMap<Integer, BlockingQueue<SensorEvent>>();
		consumers = new HashMap<Integer, Thread>();
		sensors = new HashMap<Integer, Sensor>();
	}

	/**
	 * Called by the system every time a client explicitly starts the service by
	 * calling startService(Intent), providing the arguments it supplied and a
	 * unique integer token representing the start request. Do not call this
	 * method directly.
	 */
	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		activity = (String) intent.getExtras().get(ApplicationConstants.INTENT_ACTIVITY);
		showToast("Logging started. Activity: " + activity);

		finished = false;
		isRunning = true;
		startTime = SystemClock.uptimeMillis();

		/**
		 * Acquire a partial wakelock in order to allow for sensor event logging
		 * when the user presses the power button. see Android documentation: If
		 * you hold a partial wakelock, the CPU will continue to run,
		 * irrespective of any timers and even after the user presses the power
		 * button. In all other wakelocks, the CPU will run, but the user can
		 * still put the device to sleep using the power button.
		 */
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		partialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		synchronized (this) {
			partialWakeLock.acquire();
		}

		final SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		final List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		try {
			for (final Sensor sensor : sensorList) {
				if (prefs.getBoolean(sensor.getName(), false)) {
					final LinkedBlockingQueue<SensorEvent> dataQueue = new LinkedBlockingQueue<SensorEvent>();
					final Thread consumer = new Thread(new Consumer(dataQueue, getFileName(sensor.getName())));

					data.put(sensor.getType(), dataQueue);
					sensors.put(sensor.getType(), sensor);
					consumers.put(sensor.getType(), consumer);

					consumer.start();
					final int delay = Integer.parseInt(prefs.getString(ApplicationConstants.PREF_RATE,
							SensorManager.SENSOR_DELAY_NORMAL + ""));
					sensorManager.registerListener(sensorEventListener, sensor, delay);
				}
			}
		} catch (final IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return START_STICKY;
	}

	/**
	 * Return the communication channel to the service. May return null if
	 * clients can not bind to the service.
	 */
	@Override
	public IBinder onBind(final Intent arg0) {
		return mBinder;
	}

	/**
	 * Called by the system to notify a Service that it is no longer used and is
	 * being removed. The service should clean up an resources it holds
	 * (threads, registered receivers, etc) at this point. Upon return, there
	 * will be no more calls in to this Service object and it is effectively
	 * dead. Do not call this method directly.
	 */
	@Override
	public void onDestroy() {
		Log.i(TAG, "[RecorderService] onDestroy called");
		// unregister all listeners
		sensorManager.unregisterListener(sensorEventListener);

		// stop all consumer threads
		finished = true;
		for (final Thread t : consumers.values()) {
			t.interrupt();
		}

		// release the wake lock
		synchronized (this) {
			partialWakeLock.release();
		}

		// update state
		isRunning = false;

		// show a toast that the service has been stopped
		showToast("logging stopped");
	}

	/* ***************************************************************
	 * helper methods
	 */

	/**
	 * This method just shows a toast (i.e. a small pop-up message) displaying
	 * the specified message.
	 * 
	 * @param message
	 *            The message to be shown.
	 */
	private void showToast(final String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	private String getFileName(final String sensorName) {
		final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy_HH-mm-ss");
		return dateFormat.format(new Date()) + "_" + sensorName + ".csv";
	}

	/* ***************************************************************
	 * helper classes
	 */

	// consumer threads will check this variable
	private volatile boolean finished = false;

	private PowerManager.WakeLock partialWakeLock;

	/**
	 * Writes the contents of the specified queue to the specified file.
	 * 
	 * @param queue
	 *            The data queue
	 * @param file
	 *            The target file.
	 */

	private class Consumer implements Runnable {
		private final BlockingQueue<SensorEvent> queue;
		private BufferedWriter out;

		public Consumer(final BlockingQueue<SensorEvent> queue, final String fileName) throws IOException {
			this.setFile(fileName);
			this.queue = queue;
			out.write("# Logged activity: " + activity + "\n");
		}

		@Override
		public void run() {
			Log.i(TAG, this.toString() + ".run()");
			while (!finished) {
				try {
					out.write(toCSVString(queue.take()));
				} catch (final IOException e) {
					Log.e(TAG, e.getMessage(), e);
				} catch (final InterruptedException e) {
					continue;
				}
			}

			cleanUp();
		}

		private void cleanUp() {
			Log.i(TAG, this.toString() + ".cleanUp()");

			try {
				while (!queue.isEmpty()) {
					out.write(toCSVString(queue.take()));
				}
				out.flush();
				out.close();
			} catch (final IOException e) {
				Log.e(TAG, e.getMessage(), e);
			} catch (final InterruptedException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}

		private void setFile(final String fileName) {
			try {
				if (out != null) {
					out.close();
				}
				final File file = new File(getExternalFilesDir(null), fileName);
				if (!file.exists()) {
					file.createNewFile();
				}
				out = new BufferedWriter(new FileWriter(file));
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		private String toCSVString(final SensorEvent event) {
			// nano-seconds are too fine grained --> convert it to
			// milli-seconds.
			return (event.timestamp / (1000 * 1000) - startTime) + "," + event.values[0] + ", " + event.values[1] + ","
					+ event.values[2] + "\n";
		}
	}

	public class RecorderBinder extends Binder {
		public RecorderService getService() {
			// Return this instance of RecorderService so clients can call
			// public methods
			return RecorderService.this;
		}

	}
}
