package com.example.pavl.tabstest.data;

import android.provider.BaseColumns;

/**
 * Created by pavl on 27.08.2016.
 */
public final class SQLiteContracts {
    private SQLiteContracts() {}

    private static final String BLOB_TYPE = " BLOB";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";

    //users table contract
    public static final String SQL_CREATE_USERS_ENTRIES =
            "CREATE TABLE " + UsersTable.TABLE_NAME + " (" +
                    UsersTable._ID + " INTEGER PRIMARY KEY," +
                    UsersTable.USER_ID + INTEGER_TYPE + COMMA_SEP +
                    UsersTable.USER_BYTES + BLOB_TYPE + " )";

    public static final String SQL_DELETE_USERS_ENTRIES =
            "DROP TABLE IF EXISTS " + UsersTable.TABLE_NAME;

    public static class UsersTable implements BaseColumns {
        private UsersTable() {}

        public static final String TABLE_NAME = "vk_users";
        public static final String USER_ID = "user_id";
        public static final String USER_BYTES = "user_bytes";
    }

    //dialogs table contract
    public static final String SQL_CREATE_DIALOGS_ENTRIES =
            "CREATE TABLE " + DialogsTable.TABLE_NAME + " (" +
                    DialogsTable._ID + " INTEGER PRIMARY KEY," +
                    DialogsTable.FROM_USER_ID + INTEGER_TYPE + COMMA_SEP +
                    DialogsTable.TO_USER_ID + INTEGER_TYPE + COMMA_SEP +
                    DialogsTable.DIALOG_BYTES + BLOB_TYPE + " )";

    public static final String SQL_DELETE_DIALOGS_ENTRIES =
            "DROP TABLE IF EXISTS " + DialogsTable.TABLE_NAME;

    public static class DialogsTable implements BaseColumns {
        private DialogsTable() {}

        public static final String TABLE_NAME = "vk_dialogs";
        public static final String FROM_USER_ID = "from_id";
        public static final String TO_USER_ID = "to_id";
        public static final String DIALOG_BYTES = "dialog_bytes";
    }

    //dialogs table contract
    public static final String SQL_CREATE_MESSAGES_ENTRIES =
            "CREATE TABLE " + MessagesTable.TABLE_NAME + " (" +
                    MessagesTable._ID + " INTEGER PRIMARY KEY," +
                    MessagesTable.FROM_USER_ID + INTEGER_TYPE + COMMA_SEP +
                    MessagesTable.TO_USER_ID + INTEGER_TYPE + COMMA_SEP +
                    MessagesTable.MESSAGE_BYTES + BLOB_TYPE + " )";

    public static final String SQL_DELETE_MESSAGES_ENTRIES =
            "DROP TABLE IF EXISTS " + MessagesTable.TABLE_NAME;

    public static class MessagesTable implements BaseColumns {
        private MessagesTable() {}

        public static final String TABLE_NAME = "vk_messages";
        public static final String FROM_USER_ID = "from_id";
        public static final String TO_USER_ID = "to_id";
        public static final String MESSAGE_BYTES = "message_bytes";
    }
}
