package com.example.pavl.tabstest.data;

import com.vk.sdk.api.model.VKApiMessage;

import java.util.List;
import java.util.Observable;

/**
 * Created by pavl on 23.08.2016.
 */
public class HistoryDataChangedNotifier extends Observable {

    private static HistoryDataChangedNotifier ourInstance = new HistoryDataChangedNotifier();

    public static HistoryDataChangedNotifier getInstance() {
        return ourInstance;
    }

    private HistoryDataChangedNotifier() {
    }

    //TODO store history by two ids (from|to) or clean it after relogin
    public void saveHistory(String userId, List<VKApiMessage> data) {
        DataHandler.getInstance().saveHistory(userId, data);
        setChanged();
        notifyObservers();
    }

    public List<VKApiMessage> getHistory(String user_id) {
        return DataHandler.getInstance().getHistory(user_id);
    }

    public void addToHistory(int userId, List<VKApiMessage> messages) {
        DataHandler.getInstance().addToHistory(userId, messages);
        setChanged();
        notifyObservers();
    }
}
