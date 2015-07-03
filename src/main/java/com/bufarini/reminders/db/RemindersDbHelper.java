package com.bufarini.reminders.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RemindersDbHelper extends SQLiteOpenHelper {

	  private static final String DATABASE_NAME = "todotable.db";
	  private static final int DATABASE_VERSION = 1;

	  public RemindersDbHelper(Context context) {
	    super(context, DATABASE_NAME, null, DATABASE_VERSION);
	  }

	  // Method is called during creation of the database
	  @Override
	  public void onCreate(SQLiteDatabase database) {
	    Tables.onCreate(database);
	  }

	  // Method is called during an upgrade of the database,
	  // e.g. if you increase the database version
	  @Override
	  public void onUpgrade(SQLiteDatabase database, int oldVersion,
	      int newVersion) {
		  Tables.onUpgrade(database, oldVersion, newVersion);
	  }
	} 
