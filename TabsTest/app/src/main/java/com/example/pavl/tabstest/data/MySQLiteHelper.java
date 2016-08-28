package com.example.pavl.tabstest.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.vk.sdk.api.model.VKApiDialog;
import com.vk.sdk.api.model.VKApiUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pavl on 27.08.2016.
 */
public class MySQLiteHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "vk_messenger.db";
    private static final String TAG = MySQLiteHelper.class.getName();

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createDBTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        deleteDBTables(db);
        onCreate(db);
    }

    public void deleteDBTables(SQLiteDatabase db) {
        db.execSQL(SQLiteContracts.SQL_DELETE_USERS_ENTRIES);
        db.execSQL(SQLiteContracts.SQL_DELETE_DIALOGS_ENTRIES);
        db.execSQL(SQLiteContracts.SQL_DELETE_MESSAGES_ENTRIES);
    }

    public void createDBTables(SQLiteDatabase db) {
        db.execSQL(SQLiteContracts.SQL_CREATE_USERS_ENTRIES);
        db.execSQL(SQLiteContracts.SQL_CREATE_DIALOGS_ENTRIES);
        db.execSQL(SQLiteContracts.SQL_CREATE_MESSAGES_ENTRIES);
    }

    //TODO use it temporary
    public void dropDB(SQLiteDatabase db) {
        deleteDBTables(db);
        createDBTables(db);
    }

    //----------------------------
    //Next methods are DAO methods

    //using on app pause
    public static class TaskSetDialogsAndUsersToDBAsync extends AsyncTask<Context, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Context... params) {
            Context context = params[0];
            if (context == null) return false;
            setDataToDb(context);
            return true;
        }

        //TODO can handle result in onPostExecute
    }

    private static void setDataToDb(Context context) throws SQLException {
        MySQLiteHelper dbHelper = new MySQLiteHelper(context);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        dbHelper.dropDB(db);

        int  ownerId = DataHandler.getInstance().getOwnerId();
        //insert Dialogs
        List<VKApiDialog> dialogs = DataHandler.getInstance().getDialogList();
        for (VKApiDialog item : dialogs) {
            insertDialogToDB(db, ownerId, item.message.user_id,
                    marshall(item));
        }
        //insert Users
        List<VKApiUser> users = DataHandler.getInstance().getUserList();
        for (VKApiUser item : users) {
            insertUserToDB(db, item.getId(),
                    marshall(item));
        }

        db.close();
    }

    public static class TaskGetDialogsAndUsersFromDBAsync extends AsyncTask<Context, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Context... params) {
            Context context = params[0];
            if (context == null) return false;
            getDialogsAndUsersFromDB(context);
            return true;
        }

        //TODO can handle result in onPostExecute
    }

    private static void getDialogsAndUsersFromDB(Context context) {
        if (context == null) return;
        MySQLiteHelper dbHelper = new MySQLiteHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.query(SQLiteContracts.DialogsTable.TABLE_NAME,
                new String[]{SQLiteContracts.DialogsTable.DIALOG_BYTES},
                SQLiteContracts.DialogsTable.FROM_USER_ID + " = ?",
                new String[]{String.valueOf(DataHandler.getInstance().getOwnerId())},
                null, null, null);

        //get Dialogs
        List<VKApiDialog> dialogs = new ArrayList<>();
        if (c.moveToFirst()) {
            do {
                VKApiDialog item = unmarshall(c.getBlob(0), VKApiDialog.CREATOR);
                Log.i(TAG, "mister, you got dialog " + item.message.body);
            } while (c.moveToNext());
        }
        c.close();

        //get Users
        c = db.query(SQLiteContracts.UsersTable.TABLE_NAME,
                new String[]{SQLiteContracts.UsersTable.USER_BYTES},
                null, null, null, null, null);

        List<VKApiUser> users = new ArrayList<>();
        if (c.moveToFirst()) {
            do {
                VKApiUser item = unmarshall(c.getBlob(0), VKApiUser.CREATOR);
                Log.i(TAG, "mister, you got user " + item.last_name);
            } while (c.moveToNext());
        }
        c.close();

        if (dialogs.size() != 0 && users.size() != 0) DialogsDataChangedNotifier.getInstance().saveDialogsAndUsers(dialogs, users);

        db.close();
    }

    public static byte[] marshall(Parcelable parceable) {
        Parcel parcel = Parcel.obtain();
        parceable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    public static <T extends Parcelable> T unmarshall(byte[] bytes, Parcelable.Creator<T> creator) {
        Parcel parcel = unmarshall(bytes);
        return creator.createFromParcel(parcel);
    }

    public static Parcel unmarshall(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        return parcel;
    }

    //insert methods etc
    private static void insertUserToDB(SQLiteDatabase db, int userId, byte[] userBytes) {
        ContentValues userValues = new ContentValues();
        userValues.put(SQLiteContracts.UsersTable.USER_ID, userId);
        userValues.put(SQLiteContracts.UsersTable.USER_BYTES, userBytes);
        db.insert(SQLiteContracts.UsersTable.TABLE_NAME, null, userValues);
    }

    private static void insertDialogToDB(SQLiteDatabase db, int fromId, int toId, byte[] dialogBytes) {
        ContentValues dialogValues = new ContentValues();
        dialogValues.put(SQLiteContracts.DialogsTable.FROM_USER_ID, fromId);
        dialogValues.put(SQLiteContracts.DialogsTable.TO_USER_ID, toId);
        dialogValues.put(SQLiteContracts.DialogsTable.DIALOG_BYTES, dialogBytes);
        db.insert(SQLiteContracts.DialogsTable.TABLE_NAME, null, dialogValues);
    }

    private static void insertMessagesToDB(SQLiteDatabase db, int fromId, int toId, byte[] messageBytes) {
        ContentValues messageValues = new ContentValues();
        messageValues.put(SQLiteContracts.MessagesTable.FROM_USER_ID, fromId);
        messageValues.put(SQLiteContracts.MessagesTable.TO_USER_ID, toId);
        messageValues.put(SQLiteContracts.MessagesTable.MESSAGE_BYTES, messageBytes);
        db.insert(SQLiteContracts.MessagesTable.TABLE_NAME, null, messageValues);
    }

    //will used for store data without drop the DB
    //private static void updateDialogToDB(SQLiteDatabase db, int fromId, int toId, byte[] dialogBytes) {
    //    ContentValues dialogValues = new ContentValues();
    //    dialogValues.put(SQLiteContracts.DialogsTable.FROM_USER_ID, fromId);
    //    dialogValues.put(SQLiteContracts.DialogsTable.TO_USER_ID, toId);
    //    dialogValues.put(SQLiteContracts.DialogsTable.DIALOG_BYTES, dialogBytes);

    //    int effected = db.update(SQLiteContracts.DialogsTable.TABLE_NAME, dialogValues,
    //            SQLiteContracts.DialogsTable.FROM_USER_ID + " = ? AND " +
    //                    SQLiteContracts.DialogsTable.TO_USER_ID + " = ?",
    //            new String[] {String.valueOf(fromId), String.valueOf(toId)});

    //    if (effected < 1) db.insert(SQLiteContracts.DialogsTable.TABLE_NAME, null, dialogValues);
    //}
}
