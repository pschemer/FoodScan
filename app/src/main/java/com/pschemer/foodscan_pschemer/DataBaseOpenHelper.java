package com.pschemer.foodscan_pschemer;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBaseOpenHelper extends SQLiteOpenHelper {
    final private static String TABLE_NAME = "foodscans";
    final private static String CREATE_CMD =
        "CREATE TABLE " + TABLE_NAME +" (" + ScanCode._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ScanCode.UPC + " TEXT NOT NULL, "
                + ScanCode.NAME + " TEXT, "
                + ScanCode.LABELS + " TEXT)";

    final private static Integer VERSION = 2;
    final private Context context;

    public DataBaseOpenHelper(Context context) {
        super(context, TABLE_NAME, null, VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CMD);
        // initial values
        ContentValues cv = new ContentValues(2);
        cv.put(ScanCode.UPC, "307667491852");
        cv.put(ScanCode.NAME, "TEST - CHEWY TUMS");
        cv.put(ScanCode.LABELS, "VEGAN__,__FAT_FREE__,__NOI_OIL_ADDED");
        db.insert(ScanCode.TABLE_NAME, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    void deleteDatabase ( ) {
        context.deleteDatabase(TABLE_NAME);
    }
}

