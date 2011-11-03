//package at.jku.cp.feichtinger.sensorLogger.activities;
//
//import android.app.Activity;
//import android.hardware.Sensor;
//import android.hardware.SensorEvent;
//import android.hardware.SensorEventListener;
//import android.hardware.SensorManager;
//import android.os.Bundle;
//import android.widget.TextView;
//import at.jku.cp.feichtinger.sensorLogger.ApplicationConstants;
//import at.jku.cp.feichtinger.sensorLogger.EnumeratedSensor;
//import at.jku.cp.feichtinger.sensorLogger.R;
//
//public class SensorVisualizerActivity extends Activity {
//	/* sensor stuff ***************************** */
//	private Sensor currentSensor;
//	private SensorManager sensorManager;
//
//	/* ui stuff ********************************* */
//	private TextView timestamp;
//	private TextView xAxis;
//	private TextView yAxis;
//	private TextView zAxis;
//
//	/**
//	 * The sensor event listener records acceleration data and puts it into the
//	 * graph. Only the last <code>MAX_DISPLAY_SIZE</code> elements will be
//	 * displayed, older values will be removed. This does not monitor data, it
//	 * just displays it.
//	 */
//	private final SensorEventListener sensorEventListener = new SensorEventListener() {
//		/**
//		 * Called when sensor values have changed.
//		 */
//		public void onSensorChanged(final SensorEvent event) {
//			if (event.sensor.getType() == currentSensor.getType()) {
//				float xValue = event.values[0];
//				float yValue = event.values[1];
//				float zValue = event.values[2];
//
//				timestamp.setText("" + event.timestamp);
//				xAxis.setText("" + xValue);
//				yAxis.setText("" + yValue);
//				zAxis.setText("" + zValue);
//			}
//		}
//
//		public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
//			// TODO Auto-generated method stub
//		}
//	};
//
//	/* ********************************************
//	 * Activity life-cylce methods
//	 */
//
//	@Override
//	public void onCreate(final Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.visualizer);
//
//		initUi();
//
//		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//		final EnumeratedSensor sensor = (EnumeratedSensor) getIntent().getExtras().getSerializable(
//				ApplicationConstants.SENSOR);
//		currentSensor = sensorManager.getDefaultSensor(sensor.getSensorId());
//	}
//
//	@Override
//	protected void onResume() {
//		super.onResume();
//		sensorManager.registerListener(sensorEventListener, currentSensor, SensorManager.SENSOR_DELAY_NORMAL);
//	}
//
//	@Override
//	protected void onStop() {
//		super.onStop();
//		sensorManager.unregisterListener(sensorEventListener);
//	}
//
//	/* *****************************************
//	 * initialization
//	 */
//
//	private void initUi() {
//		timestamp = (TextView) findViewById(R.id.timestamp);
//		xAxis = (TextView) findViewById(R.id.xAxis);
//		yAxis = (TextView) findViewById(R.id.yAxis);
//		zAxis = (TextView) findViewById(R.id.zAxis);
//	}
//
// }
