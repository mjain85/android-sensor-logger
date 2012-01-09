package at.feichtinger.sensorlogger.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import at.feichtinger.sensorlogger.R;
import at.feichtinger.sensorlogger.activities.LoggerActivity;

/**
 * A service for logging sensor values.
 * 
 * @author Thomas Feichtinger
 */
public class SensorLoggerService extends Service {
	private static final String TAG = "at.feichtinger.sensorlogger.services.SensorLoggerService";
	public static final String DATA_ACTIVITY = "data_activity";
	public static final String DATA_STATUS = "data_status";

	/**
	 * Handler of incoming messages from clients.
	 */
	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				// register the client
				mClient = msg.replyTo;
				break;
			case MSG_SET_ACTIVITY_AND_START_LOGGING:
				// if we are currently logging then we need to stop it before we
				// can change the activity
				if (state == ServiceState.LOGGING) {
					stopLogging();
				}
				try {
					startLogging(msg.getData());
				} catch (IOException e) {
					Log.e(TAG, e.getMessage(), e);
				}
				break;
			case MSG_STOP_LOGGING:
				stopLogging();
				break;
			}

			informClientCurrentStatus();
		}

		private void informClientCurrentStatus() {
			// inform the client that the activity has been set
			try {
				final Message message = Message.obtain(null, MSG_STATUS);
				final Bundle data = new Bundle();
				data.putString(DATA_STATUS, state.toString());
				data.putString(DATA_ACTIVITY, currentActivity);
				message.setData(data);
				mClient.send(message);
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
		}

		private void startLogging(final Bundle data) throws IOException {
			currentActivity = data.getString(DATA_ACTIVITY);
			referenceTime = SystemClock.uptimeMillis();
			startTime = new Date();

			int linearAccSensor = Sensor.TYPE_LINEAR_ACCELERATION;
			int gravitySensor = Sensor.TYPE_GRAVITY;
			sensorMap.put(linearAccSensor, new BufferedWriter(new FileWriter(getLogFile(linearAccSensor))));
			sensorMap.put(gravitySensor, new BufferedWriter(new FileWriter(getLogFile(gravitySensor))));

			for (BufferedWriter writer : sensorMap.values()) {
				writer.write("#" + currentActivity.toLowerCase().replace(" ", "") + "\n");
				writer.write("time[ms], x-axis[m/s^2], y-axis[m/s^2], z-axis[m/s^2]\n");
			}

			mSensorManager.registerListener(sensorEventListener, mSensorManager.getSensorList(linearAccSensor).get(0),
					SensorManager.SENSOR_DELAY_FASTEST);
			mSensorManager.registerListener(sensorEventListener, mSensorManager.getSensorList(gravitySensor).get(0),
					SensorManager.SENSOR_DELAY_FASTEST);

			state = ServiceState.LOGGING;
			showNotification();
		}

		private File getLogFile(int sensorType) throws IOException {
			final String fileName = getFileName(mSensorManager.getSensorList(sensorType));
			final File file = new File(getExternalFilesDir(null), fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
			Log.i(TAG, "file created: " + file.getAbsolutePath());
			return file;
		}

		private void stopLogging() {
			state = ServiceState.STOPPED;
			// first stop logging new values
			mSensorManager.unregisterListener(sensorEventListener);
			for (BufferedWriter writer : sensorMap.values()) {
				try {
					writer.flush();
					writer.close();
				} catch (IOException e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}
			// remove notification (using the unique id of our string that was
			// used when we created the notification)
			mNotificationManager.cancel(R.string.logger_service_started);
		}

		private String getFileName(final List<Sensor> sensors) {
			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmssSSS");
			String filename = dateFormat.format(startTime) + "-" + currentActivity.toLowerCase().replace(" ", "") + "-";
			for (Sensor s : sensors) {
				filename += s.getName().toLowerCase().replace(" ", "");
			}
			return filename + ".csv";
		}
	}

	public enum ServiceState {

		/**
		 * The service is in state logging iff the sensor event listener is
		 * registered.
		 */
		LOGGING,
		/**
		 * The service starts in this state. No logging is going on in this
		 * state.
		 */
		STOPPED;
	}

	/* ********************************************************************
	 * Commands for this service
	 */

	public final static int MSG_REGISTER_CLIENT = 0;

	/**
	 * Command to the service to start logging. Sends callbacks to the client.
	 * The Message's replyTo field must be a Messenger of the client where
	 * callbacks should be sent
	 */
	public final static int MSG_SET_ACTIVITY_AND_START_LOGGING = 1;

	/**
	 * Command to the service to stop logging. Sends callbacks to the client.
	 * The Message's replyTo field must be a Messenger of the client where
	 * callbacks should be sent
	 */
	public final static int MSG_STOP_LOGGING = 2;

	/**
	 * Command to the service to send back its current status. The client will
	 * receive an answer with the same MSG constant.
	 */
	public final static int MSG_STATUS = 3;

	/* **************************************************************************
	 * Fields
	 */

	/** */
	private ServiceState state = ServiceState.STOPPED;

	/** Remembers the activity for which the sensors are being logged. */
	private String currentActivity;

	/** Target published for clients to send messages to. */
	private Messenger mMessenger;

	/** The notification manager. Used to show notifications. */
	private NotificationManager mNotificationManager;

	/** The sensor manager. Used to access sensors. */
	private SensorManager mSensorManager;

	/** The attached client, there can only one at any given time. */
	private Messenger mClient;

	/**
	 * Keeps track of the system time when logging was started. Used to
	 * calculate relative time offsets of each sensor event.
	 */
	private long referenceTime;

	/**
	 * Remembers when logging started. Used for creating a file name.
	 */
	private Date startTime;

	/**
	 * Used to keep phone listening to sensor values when the screen is turned
	 * off.
	 */
	private PowerManager.WakeLock partialWakeLock;

	/**
	 * Stores the sensor events of each sensor in a queue. Keys are the IDs of
	 * the used sensors. The SensorEventListener will store the events in this
	 * map. A blocking queue is used to store the events because its content
	 * will be written to files by a separate thread.
	 */
	private Map<Integer, BufferedWriter> sensorMap;

	/**
	 * The sensor event listener. Puts sensor values into the corresponding
	 * queue (depending on the sensor type).
	 */
	private final SensorEventListener sensorEventListener = new SensorEventListener() {
		/**
		 * Conversion factor from nanoseconds to milliseconds
		 * 
		 * <pre>
		 * nano  = 10^-9 
		 * mikro = 10^-6
		 * milli = 10^-3
		 * </pre>
		 */
		private final static int CONVERSION_FACTOR = 1000 * 1000;

		@Override
		public void onSensorChanged(final SensorEvent event) {
			// Log.i(TAG, "onSensorChanged called:" + toCSVString(event));

			try {
				// get the corresponding queue
				final BufferedWriter writer = sensorMap.get(event.sensor.getType());
				if (state == ServiceState.LOGGING) {
					writer.write(toCSVString(event));
				}
			} catch (IOException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}

		@Override
		public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
			// TODO Auto-generated method stub
		}

		/**
		 * Creates a .csv representation out of a sensor event.
		 * 
		 * @param event
		 * @return
		 */
		private String toCSVString(final SensorEvent event) {
			return (event.timestamp / CONVERSION_FACTOR) - referenceTime + "," + event.values[0] + ", "
					+ event.values[1] + "," + event.values[2] + "\n";
		}
	};

	/* **************************************************************************
	 * Service life-cycle
	 */

	/**
	 * Called by the system when the service is first created. Do not call this
	 * method directly.
	 */
	@Override
	public void onCreate() {
		// instantiate system services
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		// instantiate private fields
		mMessenger = new Messenger(new IncomingHandler());
		sensorMap = new HashMap<Integer, BufferedWriter>();

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
	}

	@Override
	public void onDestroy() {
		// release the wake lock
		synchronized (this) {
			partialWakeLock.release();
		}
	}

	/* ************************************************************************* */

	/**
	 * Return the messenger interface upon binding to this service. Allows the
	 * client to communicate with this service via the messenger interface.
	 */
	@Override
	public IBinder onBind(final Intent intent) {
		return mMessenger.getBinder();
	}

	/* **************************************************************************
	 * Private helper methods
	 */

	/**
	 * Displays a notification as long as this service is logging sensor values.
	 * The user will be redirected to the logging activity if he reacts to the
	 * notification.
	 */
	private void showNotification() {
		// Text being displayed for the ticker and expanded notification
		final CharSequence notificationText = getText(R.string.logger_service_started);

		// set the icon, scrolling text and timestamp
		final Notification notification = new Notification(R.drawable.plot, notificationText,
				System.currentTimeMillis());
		notification.flags |= Notification.FLAG_NO_CLEAR;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;

		// the PendingIntent used to launch the logger activity if the user
		// selects this notification
		final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, LoggerActivity.class),
				0);

		notification.setLatestEventInfo(this, getText(R.string.app_name), notificationText, pendingIntent);

		// send the notification. string id used because its a unique number.
		mNotificationManager.notify(R.string.logger_service_started, notification);
	}
}
