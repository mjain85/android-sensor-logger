package at.jku.cp.feichtinger.sensorLogger;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ToggleButton;
import at.jku.cp.feichtinger.sensorLogger.activities.PreferencesActivity;
import at.jku.cp.feichtinger.sensorLogger.services.RecorderService;

public class SensorLoggerMainActivity extends Activity {
	private static final String TAG = "at.jku.cp.feichtinger.sensorLogger.SensorLoggerMainActivity";
	private ArrayAdapter<String> listAdapter;
	// private ImageView recordingButton;
	private ToggleButton recordingButton;
	private ListView sensorList;

	/* ********************************************
	 * Listeners
	 */
	final OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			updateSensorList();
		}
	};

	final OnItemClickListener listViewClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
			// start a sensor visualizer activity for the selected sensor
			// final Intent intent = new Intent(SensorLoggerMainActivity.this,
			// SensorVisualizerActivity.class);
			// startActivity(intent);
		}
	};

	/* ********************************************
	 * Menu methods
	 */

	/**
	 * This hook is called whenever an item in your options menu is selected.
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.settings:
			startActivity(new Intent(getBaseContext(), PreferencesActivity.class));
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Initialize the contents of the Activity's standard options menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	/* ********************************************
	 * Activity life-cylce methods
	 */

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

		initUI();
	}

	@Override
	protected void onResume() {
		super.onResume();
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
		updateSensorList();
	}

	@Override
	protected void onStop() {
		super.onStop();
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
	}

	/* *****************************************
	 * initialization
	 */

	private void updateSensorList() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		final List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);

		for (Sensor s : sensorList) {
			if (prefs.getBoolean(s.getName(), false)) {
				listAdapter.add(s.getName());
			}
		}
	}

	private void initUI() {
		sensorList = (ListView) findViewById(R.id.sensorList);
		sensorList.setOnItemClickListener(listViewClickListener);
		// sensorList.addHeaderView(findViewById(R.id.headerId));
		sensorList.setAdapter(listAdapter);
		// recordingButton = (ImageView) findViewById(R.id.recordingButton);
		recordingButton = (ToggleButton) findViewById(R.id.recordingButton);
	}

	/* *****************************************
	 * UI callback methods
	 */

	public void toggleRecording(final View view) {
		Log.i(TAG, "[Main] toggleRecording called.");
		if (RecorderService.isRunning()) {
			stopService();
			recordingButton.setSelected(false);
			// recordingButton.setImageResource(R.drawable.recorddisabled);
		} else {
			startService();
			recordingButton.setSelected(true);
			// recordingButton.setImageResource(R.drawable.recordpressed);
		}
	}

	/* *****************************************
	 * private helper methods
	 */

	/**
	 * Stops the accelerometer stopping service.
	 * 
	 * @return the new recording status - always false after stopping
	 */
	private boolean stopService() {
		stopService(new Intent(SensorLoggerMainActivity.this, RecorderService.class));
		return false;
	}

	/**
	 * Starts the accelerometer stopping service.
	 * 
	 * @return the new recording status - always true after starting
	 */
	private boolean startService() {
		final Intent intent = new Intent(SensorLoggerMainActivity.this, RecorderService.class);
		startService(intent);
		return true;
	}
}