package at.jku.cp.feichtinger.sensorLogger;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ToggleButton;
import at.jku.cp.feichtinger.sensorLogger.activities.ActivityManagerActivity;
import at.jku.cp.feichtinger.sensorLogger.activities.PreferencesActivity;
import at.jku.cp.feichtinger.sensorLogger.services.RecorderService;

public class SensorLoggerMainActivity extends Activity {
	private static final String TAG = "at.jku.cp.feichtinger.sensorLogger.SensorLoggerMainActivity";
	private ArrayAdapter<String> listAdapter;
	private ToggleButton recordingButton;
	private ListView sensorList;

	/* ********************************************
	 * Listeners
	 */
	final OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
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
			break;
		case R.id.manage_activities:
			startActivity(new Intent(getBaseContext(), ActivityManagerActivity.class));
			break;
		}
		return super.onOptionsItemSelected(item);
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
		listAdapter.clear();
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		final List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);

		for (final Sensor s : sensorList) {
			if (prefs.getBoolean(s.getName(), false)) {
				listAdapter.add(s.getName());
			}
		}
	}

	private void initUI() {
		sensorList = (ListView) findViewById(R.id.sensorList);
		sensorList.setOnItemClickListener(listViewClickListener);
		listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		sensorList.setAdapter(listAdapter);
		recordingButton = (ToggleButton) findViewById(R.id.recordingButton);
		updateSensorList();
	}

	/* *****************************************
	 * UI callback methods
	 */

	public void toggleRecording(final View view) {
		if (RecorderService.isRunning()) {
			stopService();
			recordingButton.setSelected(false);
		} else {
			final String[] items = getResources().getStringArray(R.array.activities);
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Pick an activity");
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, final int item) {
					startService(items[item]);
					recordingButton.setSelected(true);
				}
			});
			final AlertDialog alert = builder.create();
			alert.show();
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
	private boolean startService(final String activity) {
		final Intent intent = new Intent(SensorLoggerMainActivity.this, RecorderService.class);
		intent.putExtra(ApplicationConstants.INTENT_ACTIVITY, activity);
		startService(intent);
		return true;
	}
}