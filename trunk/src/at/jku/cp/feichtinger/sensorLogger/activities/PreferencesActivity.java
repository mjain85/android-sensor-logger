package at.jku.cp.feichtinger.sensorLogger.activities;

import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import at.jku.cp.feichtinger.sensorLogger.R;

public class PreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		final SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		final List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
		final PreferenceCategory category = (PreferenceCategory) getPreferenceManager().findPreference("sensors");
		for (Sensor s : sensorList) {
			final CheckBoxPreference p = new CheckBoxPreference(this);
			p.setTitle(s.getName());
			p.setKey(s.getName());
			category.addPreference(p);
		}
	}
}
