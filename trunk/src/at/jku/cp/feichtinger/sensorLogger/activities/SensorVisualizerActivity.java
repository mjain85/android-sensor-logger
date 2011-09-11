package at.jku.cp.feichtinger.sensorLogger.activities;

import java.io.Serializable;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.ViewFlipper;
import at.jku.cp.feichtinger.sensorLogger.R;
import at.jku.cp.feichtinger.sensorLogger.model.ApplicationConstants;
import at.jku.cp.feichtinger.sensorLogger.model.EnumeratedSensor;

public class SensorVisualizerActivity extends Activity {
	private static final int MAX_DISPLAY_SIZE = 250;

	/* graph stuff ******************************* */
	private XYMultipleSeriesDataset mDatasetXYZ;
	private XYMultipleSeriesDataset mDatasetX;
	private XYMultipleSeriesDataset mDatasetY;
	private XYMultipleSeriesDataset mDatasetZ;
	private XYMultipleSeriesRenderer mRendererXYZ;
	private XYMultipleSeriesRenderer mRendererX;
	private XYMultipleSeriesRenderer mRendererY;
	private XYMultipleSeriesRenderer mRendererZ;
	private XYSeries xseries;
	private XYSeries yseries;
	private XYSeries zseries;
	private XYSeriesRenderer xrenderer;
	private XYSeriesRenderer yrenderer;
	private XYSeriesRenderer zrenderer;

	/* sensor stuff ***************************** */
	private Sensor currentSensor;
	private SensorManager sensorManager;

	/* ui stuff ********************************* */
	private ViewFlipper viewFlipper;

	private GraphicalView mChartViewXYZ;
	private GraphicalView mChartViewX;
	private GraphicalView mChartViewY;
	private GraphicalView mChartViewZ;

	private TextView timestamp;
	private TextView xAxis;
	private TextView yAxis;
	private TextView zAxis;

	/* listeners ********************************** */
	private final OnTouchListener touchListener = new OnTouchListener() {
		private float motionBeginXValue;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:
				// remember starting x value of motion
				motionBeginXValue = event.getX();
				return true;

			case MotionEvent.ACTION_UP:
				float motionReleaseXValue = event.getX();

				if (motionReleaseXValue < motionBeginXValue) {
					viewFlipper.showPrevious();
				} else if (motionReleaseXValue > motionBeginXValue) {
					viewFlipper.showNext();
				}
				return true;
			}
			return false;
		}
	};

	/**
	 * The sensor event listener records acceleration data and puts it into the
	 * graph. Only the last <code>MAX_DISPLAY_SIZE</code> elements will be
	 * displayed, older values will be removed. This does not monitor data, it
	 * just displays it.
	 */
	private final SensorEventListener sensorEventListener = new SensorEventListener() {
		/**
		 * Called when sensor values have changed.
		 */
		public void onSensorChanged(final SensorEvent event) {
			if (event.sensor.getType() == currentSensor.getType()) {
				// TODO find something more elegant (for removing old values
				// from the chart)
				if (xseries.getItemCount() > MAX_DISPLAY_SIZE)
					xseries.remove(0);
				if (yseries.getItemCount() > MAX_DISPLAY_SIZE)
					yseries.remove(0);
				if (zseries.getItemCount() > MAX_DISPLAY_SIZE)
					zseries.remove(0);

				float xValue = event.values[0];
				float yValue = event.values[1];
				float zValue = event.values[2];

				timestamp.setText("" + event.timestamp);
				xAxis.setText("" + xValue);
				yAxis.setText("" + yValue);
				zAxis.setText("" + zValue);

				xseries.add(event.timestamp, xValue);
				yseries.add(event.timestamp, yValue);
				zseries.add(event.timestamp, zValue);

				repaintCharts();
			}
		}

		private void repaintCharts() {
			int orientation = getResources().getConfiguration().orientation;
			if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mChartViewXYZ.repaint();
			} else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				mChartViewX.repaint();
				mChartViewY.repaint();
				mChartViewZ.repaint();
			}
		}

		public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
			// TODO Auto-generated method stub
		}
	};

	/* ********************************************
	 * Activity life-cylce methods
	 */

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.visualizer);

		initDataSet();
		initRenderer();
		initUi();

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		final EnumeratedSensor sensor = (EnumeratedSensor) getIntent().getExtras().getSerializable(
				ApplicationConstants.SENSOR);
		currentSensor = sensorManager.getDefaultSensor(sensor.getSensorId());
	}

	@Override
	protected void onResume() {
		super.onResume();
		initChartView();
		sensorManager.registerListener(sensorEventListener, currentSensor, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	protected void onStop() {
		super.onStop();
		sensorManager.unregisterListener(sensorEventListener);
	}

	/* *****************************************
	 * initialization
	 */

	private void initChartView() {
		int orientation = getResources().getConfiguration().orientation;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			if (mChartViewXYZ == null) {
				final LinearLayout layout = (LinearLayout) findViewById(R.id.chartXYZ);
				mChartViewXYZ = ChartFactory.getLineChartView(this, mDatasetXYZ, mRendererXYZ);
				layout.addView(mChartViewXYZ, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			} else {
				mChartViewXYZ.repaint();
			}
		}

		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			if (mChartViewX == null) {
				final LinearLayout layout = (LinearLayout) findViewById(R.id.chartX);
				mChartViewX = ChartFactory.getLineChartView(this, mDatasetX, mRendererX);
				layout.addView(mChartViewX, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			} else {
				mChartViewX.repaint();
			}

			if (mChartViewY == null) {
				final LinearLayout layout = (LinearLayout) findViewById(R.id.chartY);
				mChartViewY = ChartFactory.getLineChartView(this, mDatasetY, mRendererY);
				layout.addView(mChartViewY, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			} else {
				mChartViewY.repaint();
			}

			if (mChartViewZ == null) {
				final LinearLayout layout = (LinearLayout) findViewById(R.id.chartZ);
				mChartViewZ = ChartFactory.getLineChartView(this, mDatasetZ, mRendererZ);
				layout.addView(mChartViewZ, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			} else {
				mChartViewZ.repaint();
			}
		}
	}

	private void initDataSet() {
		mDatasetXYZ = new XYMultipleSeriesDataset();
		mDatasetX = new XYMultipleSeriesDataset();
		mDatasetY = new XYMultipleSeriesDataset();
		mDatasetZ = new XYMultipleSeriesDataset();

		xseries = new XYSeries("x-axis");
		yseries = new XYSeries("y-axis");
		zseries = new XYSeries("z-axis");

		mDatasetXYZ.addSeries(xseries);
		mDatasetXYZ.addSeries(yseries);
		mDatasetXYZ.addSeries(zseries);

		mDatasetX.addSeries(xseries);
		mDatasetY.addSeries(yseries);
		mDatasetZ.addSeries(zseries);
	}

	private void initRenderer() {
		mRendererXYZ = new XYMultipleSeriesRenderer();
		mRendererX = new XYMultipleSeriesRenderer();
		mRendererY = new XYMultipleSeriesRenderer();
		mRendererZ = new XYMultipleSeriesRenderer();

		xrenderer = new XYSeriesRenderer();
		yrenderer = new XYSeriesRenderer();
		zrenderer = new XYSeriesRenderer();

		xrenderer.setColor(Color.RED);
		yrenderer.setColor(Color.GREEN);
		zrenderer.setColor(Color.BLUE);

		mRendererXYZ.addSeriesRenderer(xrenderer);
		mRendererXYZ.addSeriesRenderer(yrenderer);
		mRendererXYZ.addSeriesRenderer(zrenderer);

		mRendererX.addSeriesRenderer(xrenderer);
		mRendererY.addSeriesRenderer(yrenderer);
		mRendererZ.addSeriesRenderer(zrenderer);

		mRendererXYZ.setShowGrid(true);
		mRendererXYZ.setAxisTitleTextSize(16);
		mRendererXYZ.setXTitle("[ns]");
		mRendererXYZ.setYTitle("[m/s^2]");
		mRendererXYZ.setLabelsTextSize(15);
		mRendererXYZ.setLegendTextSize(15);
		mRendererXYZ.setMargins(new int[] { 20, 30, 15, 0 });
		mRendererXYZ.setPointSize(10);
		mRendererXYZ.setClickEnabled(false);
	}

	private void initUi() {
		viewFlipper = (ViewFlipper) findViewById(R.id.sensorDetailViewFlipper);
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_out));

		timestamp = (TextView) findViewById(R.id.timestamp);
		xAxis = (TextView) findViewById(R.id.xAxis);
		yAxis = (TextView) findViewById(R.id.yAxis);
		zAxis = (TextView) findViewById(R.id.zAxis);

		findViewById(R.id.visualizerMainLayout).setOnTouchListener(touchListener);
	}

}
