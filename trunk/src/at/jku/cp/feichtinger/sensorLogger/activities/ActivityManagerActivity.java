package at.jku.cp.feichtinger.sensorLogger.activities;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import at.jku.cp.feichtinger.sensorLogger.R;
import at.jku.cp.feichtinger.sensorLogger.db.DatabaseAdapter;

public class ActivityManagerActivity extends ListActivity {

	private static final int DELETE = 1;
	private DatabaseAdapter dbAdapter;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manager);
		dbAdapter = new DatabaseAdapter(this);
		dbAdapter.open();
		fillData();
		registerForContextMenu(getListView());
	}

	private void fillData() {
		final Cursor notesCursor = dbAdapter.fetchAllActivities();
		startManagingCursor(notesCursor);

		// Create an array to specify the fields we want to display in the list
		// (only TITLE)
		final String[] from = new String[] { dbAdapter.KEY_TITLE };

		// and an array of the fields we want to bind those fields to (in this
		// case just text1)
		final int[] to = new int[] { android.R.id.text1 };

		// Now create a simple cursor adapter and set it to display
		final SimpleCursorAdapter activities = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1,
				notesCursor, from, to);
		setListAdapter(activities);
	}

	private void createNewActivity() {

		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("New Activity");
		alert.setMessage("Enter name for new activity:");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int whichButton) {
				final String value = input.getText().toString();
				dbAdapter.createActivity(value);
				showToast("save activity: " + value);
				fillData();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int whichButton) {
				// Canceled.
			}
		});

		alert.show();
	}

	/* ********************************************
	 * Context menu methods
	 */

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE, 0, R.string.menu_delete_activity);
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case DELETE:
			final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			dbAdapter.deleteActivity(info.id);
			fillData();
			return true;
		}
		return super.onContextItemSelected(item);
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

	private void showToast(final String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

}
