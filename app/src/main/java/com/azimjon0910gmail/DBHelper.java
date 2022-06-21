package com.azimjon0910gmail;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import java.util.Arrays;


public class DBHelper extends SQLiteOpenHelper {

    // DataBase name and version
    private static final String dbName = "TPListDataBase";
    private static final int dbVersion = 1;

    // --- DataBase tables --- //
    private static final String fieldId = "_id";
    // Table Satellites
    private static final String tableSatellites = "Satellites";
    private static final String satellitesFieldName = "Satellite_Name";
    private static final String satellitesFieldInfo = "Satellite_Info";

    // Table Transponders
    private static final String tableTP = "Transponders";
    private static final String TPFieldTP = "TP";
    private static final String TPFieldSatId = "Sat_Id";


    // Table Channels
    private static final String tableChannels = "Channels";
    private static final String channelsFieldName = "Channel_Name";
    private static final String channelsFieldTPId = "TP_Id";


    DBHelper(Context context) {
        super(context, dbName, null, dbVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query1 = "CREATE TABLE " + tableSatellites + "(" +
                fieldId + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                satellitesFieldName + " TEXT, " +
                satellitesFieldInfo + " TEXT);";

        String query2 = "CREATE TABLE " + tableTP + "(" +
                fieldId + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TPFieldTP + " TEXT NOT NULL, " +
                TPFieldSatId + " INTEGER NOT NULL" +
                ");";


        String query3 = String.format("CREATE TABLE %s" +
                "(" +
                "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "%s TEXT NOT NULL, " +
                "%s INTEGER NOT NULL" +
                ");", tableChannels, fieldId, channelsFieldName, channelsFieldTPId);

        db.execSQL(query1);
        db.execSQL(query2);
        db.execSQL(query3);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    static int insertSat(@NonNull SQLiteDatabase db, @NonNull String satName, String satInfo) {
        if (satInfo == null) satInfo = "";

        ContentValues cv = new ContentValues();

        cv.put(satellitesFieldName, satName);
        cv.put(satellitesFieldInfo, satInfo);

        return (int) db.insert(tableSatellites, null, cv);
    }

    static int insertTP(@NonNull SQLiteDatabase db, @NonNull String tp, int satId) {
        if (tp == null || tp.equals("") || satId < 0) {
            return -1;
        }

        ContentValues cv = new ContentValues();

        cv.put(TPFieldTP, tp);
        cv.put(TPFieldSatId, satId);

        return (int) db.insert(tableTP, null, cv);
    }

    static int insertChannels(@NonNull SQLiteDatabase db, @NonNull String[] channels, int tpId) {
        ContentValues cv = new ContentValues();

        int count = 0;

        db.beginTransaction();
        for (String channel : channels) {
            cv.put(channelsFieldName, channel);
            cv.put(channelsFieldTPId, tpId);
            count += db.insert(tableChannels, null, cv) > 0 ? 1 : 0;
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        return count;
    }

    static int existsSat(@NonNull SQLiteDatabase db, @NonNull String satName) {
        Cursor cursor = db.query(
                tableSatellites,
                null,
                satellitesFieldName + "=?",
                new String[]{satName},
                null,
                null,
                null);

        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    static int existsTP(@NonNull SQLiteDatabase db, int satId, @NonNull String tp) {
        Cursor cursor = db.query(
                    tableTP,
                    null,
                    TPFieldTP + "=? AND " + TPFieldSatId + "=?",
                    new String[]{tp, String.valueOf(satId)},
                    null,
                    null,
                    null);

        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    static int updateSatByName(@NonNull SQLiteDatabase db, @NonNull String satOldName, @NonNull String satNewName, String satInfo) {
        if (!satOldName.equals(satNewName))
            if (existsSat(db, satNewName) > 0) return -2;
        if (satInfo == null) satInfo = "";

        ContentValues cv = new ContentValues();

        cv.put(satellitesFieldName, satNewName);
        cv.put(satellitesFieldInfo, satInfo);

        return db.update(tableSatellites, cv, satellitesFieldName + "=?", new String[]{satOldName});
    }

    static int updateTpByTP(@NonNull SQLiteDatabase db, int satId, @NonNull String oldTP, @NonNull String newTP) {
        if (oldTP.equals(newTP)) return 1;
        if (existsTP(db, satId, newTP) > 0) return -2;

        ContentValues cv = new ContentValues();

        cv.put(TPFieldTP, newTP);

        return db.update(tableTP, cv, TPFieldTP + "=?", new String[]{oldTP});
    }

    static int updateChannelsByChannels(@NonNull SQLiteDatabase db, @NonNull String[] oldChannels, @NonNull String[] newChannels, int newTpId) {
        if (Arrays.equals(oldChannels, newChannels)) return 1;
        ContentValues cv = new ContentValues();
        int tpId = getTpIdFromChannel(db, oldChannels[0]);

        int count = 0;

        // Старые удаляем...
        for (String channel : oldChannels)
            count += db.delete(tableChannels, channelsFieldName + "=?", new String[]{String.valueOf(channel)}) > 0 ? 1 : 0;
        // Новые добавляем
        for (String channel : newChannels) {
            cv.put(channelsFieldName, channel);
            cv.put(channelsFieldTPId, newTpId);
            count += db.insert(tableChannels, null, cv) > 0 ? 1 : 0;
        }
        return count;
    }


    // GETTERS
    static String getFieldId() {
        return fieldId;
    }

    static String getTableSatellites() {
        return tableSatellites;
    }

    static String getSatellitesFieldName() {
        return satellitesFieldName;
    }

    static String getSatellitesFieldInfo() {
        return satellitesFieldInfo;
    }

    static String getTableTP() {
        return tableTP;
    }

    static String getTPFieldTP() {
        return TPFieldTP;
    }

    static String getTPFieldSatId() {
        return TPFieldSatId;
    }

    static String getTableChannels() {
        return tableChannels;
    }

    static String getChannelsFieldName() {
        return channelsFieldName;
    }

    static String getChannelsFieldTPId() {
        return channelsFieldTPId;
    }


    static int getSatIdBySatName(@NonNull SQLiteDatabase db, @NonNull String satName) {
        Cursor cursor = db.query(tableSatellites, new String[]{fieldId}, satellitesFieldName + "=?", new String[]{satName}, null, null, null);
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(fieldId));
            cursor.close();
            return id;
        }
        cursor.close();
        return -1;
    }

    static int getTpId(@NonNull SQLiteDatabase db, int satId, @NonNull String tp) {
        Cursor cursor = db.query(tableTP, new String[]{fieldId},
                TPFieldTP + "=? AND " + TPFieldSatId + "=?",
                new String[]{tp, String.valueOf(satId)},
                null,
                null,
                null);
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(fieldId));
            cursor.close();
            return id;
        }
        cursor.close();
        return -1;
    }

    static int[] getTpIdBySatId(@NonNull SQLiteDatabase db, @NonNull int satId){
        Cursor cursor  = db.query(tableTP,
                new String[]{fieldId},
                getTPFieldSatId()+"=?",
                new String[]{String.valueOf(satId)}, null, null, null);
        if (cursor.moveToFirst()) {
            int[] ids = new int[cursor.getCount()];
            int i = 0;
            do {
                ids[i++] = cursor.getInt(cursor.getColumnIndex(fieldId));
            } while (cursor.moveToNext());
            cursor.close();
            return ids;
        }
        cursor.close();
        return null;
    }

    static int getTpIdFromChannel(@NonNull SQLiteDatabase db, @NonNull String channel) {
        Cursor cursor = db.query(tableChannels, new String[]{channelsFieldTPId}, channelsFieldName + "=?", new String[]{channel}, null, null, null);
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(channelsFieldTPId));
            cursor.close();
            return id;
        }
        cursor.close();
        return -1;
    }

    static String[] getChannelsByTpId(@NonNull SQLiteDatabase db, @NonNull int tpId) {
        Cursor cursor = db.query(tableChannels,
                new String[]{channelsFieldName},
                channelsFieldTPId + "=?",
                new String[]{String.valueOf(tpId)}, null, null, null);
        if (cursor.moveToFirst()) {
            String[] channels = new String[cursor.getCount()];
            int i = 0;
            do {
                channels[i++]= cursor.getString(cursor.getColumnIndex(channelsFieldName));
            } while (cursor.moveToNext());
            cursor.close();
            return channels;
        }
        cursor.close();
        return null;
    }
}
