package at.jku.cp.feichtinger.sensorLogger.activities;

import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import at.jku.cp.feichtinger.sensorLogger.ApplicationConstants;
import at.jku.cp.feichtinger.sensorLogger.R;

public class PreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		final SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		final List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
		final PreferenceCategory categorySensors = (PreferenceCategory) getPreferenceManager().findPreference(
				ApplicationConstants.PREF_SENSORS_CAT);
		for (final Sensor s : sensorList) {
			final CheckBoxPreference p = new CheckBoxPreference(this);
			p.setTitle(s.getName());
			p.setKey(s.getName());
			categorySensors.addPreference(p);
		}

		final PreferenceCategory categoryRate = (PreferenceCategory) getPreferenceManager().findPreference(
				ApplicationConstants.PREF_LOGGING_CAT);
		final ListPreference rate = new ListPreference(this);
		rate.setKey(ApplicationConstants.PREF_RATE);
		rate.setTitle(R.string.logging_rate);
		rate.setEntries(new String[] { getString(R.string.logging_rate_fastest), getString(R.string.logging_rate_game),
				getString(R.string.logging_rate_normal), getString(R.string.logging_rate_ui) });
		rate.setEntryValues(new String[] { SensorManager.SENSOR_DELAY_FASTEST + "",
				SensorManager.SENSOR_DELAY_GAME + "", SensorManager.SENSOR_DELAY_NORMAL + " ",
				SensorManager.SENSOR_DELAY_UI + "" });
		categoryRate.addPreference(rate);
	}
}
