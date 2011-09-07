package at.jku.cp.feichtinger.sensorLogger.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import at.jku.cp.feichtinger.sensorLogger.R;

public class PreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
