package at.jku.cp.feichtinger.sensorLogger.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseAdapter {

	public static final String KEY_TITLE = "title";
	public static final String KEY_ROWID = "_id";

	private static final String TAG = "at.jku.cp.feichtinger.sensorLogger.db.DatabaseAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE = "create table activities (_id integer primary key autoincrement, "
			+ "title text not null);";

	private static final String DATABASE_NAME = "data";
	private static final String DATABASE_TABLE = "activities";
	private static final int DATABASE_VERSION = 1;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {

			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS activities");
			onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public DatabaseAdapter(final Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the activity database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public DatabaseAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	/**
	 * Create a new activity using the title provided.
	 * 
	 * @param title
	 *            the title of the activity
	 * @return rowId or -1 if failed
	 */
	public long createActivity(final String title) {
		final ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TITLE, title);

		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Delete the activity with the given rowId
	 * 
	 * @param rowId
	 *            id of activity to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteActivity(final long rowId) {

		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all activities in the database
	 * 
	 * @return Cursor over all activities
	 */
	public Cursor fetchAllActivities() {

		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_TITLE }, null, null, null, null, null);
	}

	/**
	 * Return a Cursor positioned at the activity that matches the given rowId
	 * 
	 * @param rowId
	 *            id of activity to retrieve
	 * @return Cursor positioned to matching activity, if found
	 * @throws SQLException
	 *             if activity could not be found/retrieved
	 */
	public Cursor fetchActivity(final long rowId) throws SQLException {

		final Cursor mCursor =

		mDb.query(true, DATABASE_TABLE, new String[] { KEY_ROWID, KEY_TITLE }, KEY_ROWID + "=" + rowId, null, null,
				null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	/**
	 * Update the activity using the details provided. The activity to be
	 * updated is specified using the rowId, and it is altered to use the title
	 * and body values passed in
	 * 
	 * @param rowId
	 *            id of activity to update
	 * @param title
	 *            value to set activity title to
	 * @return true if the activity was successfully updated, false otherwise
	 */
	public boolean updateNote(final long rowId, final String title, final String body) {
		final ContentValues args = new ContentValues();
		args.put(KEY_TITLE, title);

		return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}
}
