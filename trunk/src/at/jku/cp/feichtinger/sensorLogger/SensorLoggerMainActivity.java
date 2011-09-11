package at.jku.cp.feichtinger.sensorLogger;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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
import at.jku.cp.feichtinger.sensorLogger.activities.SensorVisualizerActivity;
import at.jku.cp.feichtinger.sensorLogger.model.EnumeratedSensor;
import at.jku.cp.feichtinger.sensorLogger.services.RecorderService;

public class SensorLoggerMainActivity extends Activity {
	private static final String TAG = "at.jku.cp.feichtinger.sensorLogger.SensorLoggerMainActivity";
	private ArrayAdapter<EnumeratedSensor> listAdapter;
	private List<EnumeratedSensor> activeSensors;

	// private ImageView recordingButton;
	private ToggleButton recordingButton;
	private ListView sensorList;

	/* ********************************************
	 * Listeners
	 */

	final OnSharedPreferenceChangeListener preferencesChangedListener = new OnSharedPreferenceChangeListener() {

		@Override
		public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
			if (isSupportedSensor(key)) {

				final boolean checked = sharedPreferences.getBoolean(key, false);
				if (checked) {
					activeSensors.add(EnumeratedSensor.fromKey(key));
					listAdapter.add(EnumeratedSensor.fromKey(key));
				} else {
					activeSensors.remove(EnumeratedSensor.fromKey(key));
					listAdapter.remove(EnumeratedSensor.fromKey(key));
				}
			}
		}
	};

	final OnItemClickListener listViewClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
			// start a sensor visualizer activity for the selected sensor
			final Intent intent = new Intent(SensorLoggerMainActivity.this, SensorVisualizerActivity.class);
			intent.putExtra(ApplicationConstants.SENSOR, listAdapter.getItem(position));
			startActivity(intent);
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

		initUI();
		final SharedPreferences prefs = initPreferences();
		initSensors(prefs);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	/* *****************************************
	 * initialization
	 */

	private void initSensors(final SharedPreferences prefs) {
		activeSensors = new ArrayList<EnumeratedSensor>();

		if (prefs.getBoolean(EnumeratedSensor.GRAVITY.getKey(), false)) {
			activeSensors.add(EnumeratedSensor.GRAVITY);
			listAdapter.add(EnumeratedSensor.GRAVITY);
		}

		if (prefs.getBoolean(EnumeratedSensor.LINEAR_ACCELERATION.getKey(), false)) {
			activeSensors.add(EnumeratedSensor.LINEAR_ACCELERATION);
			listAdapter.add(EnumeratedSensor.LINEAR_ACCELERATION);
		}

		if (prefs.getBoolean(EnumeratedSensor.GYROSCOPE.getKey(), false)) {
			activeSensors.add(EnumeratedSensor.GYROSCOPE);
			listAdapter.add(EnumeratedSensor.GYROSCOPE);
		}
	}

	private SharedPreferences initPreferences() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(preferencesChangedListener);
		return prefs;
	}

	private void initUI() {
		listAdapter = new ArrayAdapter<EnumeratedSensor>(this, android.R.layout.simple_list_item_1);
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

		// TODO find better solution
		final String[] activeSensors = new String[this.activeSensors.size()];
		for (int i = 0; i < this.activeSensors.size(); i++) {
			activeSensors[i] = this.activeSensors.get(i).getKey();
		}

		intent.putExtra(ApplicationConstants.ACTIVE_SENSORS, activeSensors);
		startService(intent);

		return true;
	}

	/**
	 * Checks whether a sensor is supported by this application or not.
	 * 
	 * @param key
	 *            the sensor key
	 * @return true if the sensor is supported, false otherwise
	 */
	private boolean isSupportedSensor(final String key) {
		final EnumeratedSensor[] values = EnumeratedSensor.values();
		for (final EnumeratedSensor s : values) {
			if (s.getKey() == key) {
				return true;
			}
		}
		return false;
	}
}