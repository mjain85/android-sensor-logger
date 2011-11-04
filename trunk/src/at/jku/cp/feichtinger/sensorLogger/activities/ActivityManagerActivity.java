package at.jku.cp.feichtinger.sensorLogger.activities;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import at.jku.cp.feichtinger.sensorLogger.R;
import at.jku.cp.feichtinger.sensorLogger.db.DatabaseAdapter;

public class ActivityManagerActivity extends ListActivity {

	private DatabaseAdapter dbAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manager);
		dbAdapter = new DatabaseAdapter(this);
		dbAdapter.open();
		fillData();
	}

	private void fillData() {
		Cursor notesCursor = dbAdapter.fetchAllActivities();
		startManagingCursor(notesCursor);

		// Create an array to specify the fields we want to display in the list
		// (only TITLE)
		String[] from = new String[] { dbAdapter.KEY_TITLE };

		// and an array of the fields we want to bind those fields to (in this
		// case just text1)
		int[] to = new int[] { android.R.id.text1 };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter activities = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1,
				notesCursor, from, to);
		setListAdapter(activities);
	}

	private void createNewActivity() {
		Toast.makeText(this, "create new activity", Toast.LENGTH_SHORT).show();
		dbAdapter.createActivity("dummy");
	}

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
		case R.id.add_activity:
			createNewActivity();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Initialize the contents of the Activity's standard options menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_manager_menu, menu);
		return true;
	}

}
