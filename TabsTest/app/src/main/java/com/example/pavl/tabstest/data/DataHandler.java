package com.example.pavl.tabstest.data;

import android.util.Log;

import com.vk.sdk.api.model.VKApiDialog;
import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKApiUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pavl on 16.08.2016.
 */
public class DataHandler {
    private static final String TAG = DataHandler.class.getName();

    private static DataHandler ourInstance = new DataHandler();
    private static List<VKApiDialog> sDialogsList = Collections.synchronizedList(new ArrayList<VKApiDialog>());
    private static List<VKApiUser> sUsersList = Collections.synchronizedList(new ArrayList<VKApiUser>());
    private static Map<String, List<VKApiMessage>> sHistoryMap = Collections.synchronizedMap(new HashMap<String, List<VKApiMessage>>());
    private VKApiUser sOwner;
    private static int sOwnerId;

    public static DataHandler getInstance() {
        return ourInstance;
    }

    private DataHandler() {
        sDialogsList = Collections.synchronizedList(new ArrayList<VKApiDialog>());
        sUsersList = Collections.synchronizedList(new ArrayList<VKApiUser>());
        sHistoryMap = Collections.synchronizedMap(new HashMap<String, List<VKApiMessage>>());

        //go to db async and get data and parce it
    }

    void saveDialogsAndUsers(List<VKApiDialog> dialogs, List<VKApiUser> users) {
        saveDialogs(dialogs);
        saveUsers(users);//addUsers(users);
    }

    private void saveDialogs(List<VKApiDialog> data) {
        Log.i(TAG, "mister, dialog data storing");
        sDialogsList.clear();
        sDialogsList.addAll(data);
    }

    private void addUsers(List<VKApiUser> data) {
        Log.i(TAG, "mister, users data adding");
        sUsersList.addAll(data);
    }

    private void saveUsers(List<VKApiUser> data) {
        Log.i(TAG, "mister, users data saving");
        sUsersList.clear();
        sUsersList.addAll(data);
    }

    //TODO store history by two ids (from|to) or clean it after relogin
    void saveHistory(String user_id, List<VKApiMessage> data) {
        Log.i(TAG, "mister, users history saving");
        sHistoryMap.clear();
        sHistoryMap.put(user_id, data);
    }

    public List<VKApiMessage> getHistory(String user_id) {
        return sHistoryMap.get(user_id);
    }

    public List<VKApiDialog> getDialogList() {
        return sDialogsList;
    }

    public VKApiDialog getDialog(int listPosition) {
        return sDialogsList.get(listPosition);
    }

    public List<VKApiUser> getUserList() {
        return sUsersList;
    }

    public VKApiUser getUser(int user_id) {
        for (VKApiUser user : sUsersList) {
            if (user.getId() == user_id) return user;
        }
        return null;
    }

    public void saveOwnerUser(VKApiUser owner) {
        sOwner = owner;
        setOwnerId(sOwner.getId());
    }

    public void setOwnerId(int ownerId) {
        sOwnerId = ownerId;
    }

    public int getOwnerId() {
        return sOwnerId;
    }

    public VKApiUser getOwnerUser() {
        return sOwner;
    }

    public void addToHistory(int userId, List<VKApiMessage> messages) {
        List<VKApiMessage> list = sHistoryMap.get(String.valueOf(userId));
        for (VKApiMessage item : messages) {
            list.add(0, item);
        }
    }

}
