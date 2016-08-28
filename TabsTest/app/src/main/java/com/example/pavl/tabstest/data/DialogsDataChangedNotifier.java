package com.example.pavl.tabstest.data;

import com.vk.sdk.api.model.VKApiDialog;
import com.vk.sdk.api.model.VKApiUser;

import java.util.List;
import java.util.Observable;

/**
 * Created by pavl on 23.08.2016.
 */
public class DialogsDataChangedNotifier extends Observable {
    private static DialogsDataChangedNotifier ourInstance = new DialogsDataChangedNotifier();

    public static DialogsDataChangedNotifier getInstance() {
        return ourInstance;
    }

    private DialogsDataChangedNotifier() {
    }

    public void saveDialogsAndUsers(List<VKApiDialog> dialogs, List<VKApiUser> users) {
        DataHandler.getInstance().saveDialogsAndUsers(dialogs, users);
        setChanged();
        notifyObservers();
    }

    public List<VKApiDialog> getDialogList() {
        return DataHandler.getInstance().getDialogList();
    }

    public VKApiDialog getDialog(int idx) {
        return DataHandler.getInstance().getDialog(idx);
    }

    public List<VKApiUser> getUserList() {
        return DataHandler.getInstance().getUserList();
    }
}
