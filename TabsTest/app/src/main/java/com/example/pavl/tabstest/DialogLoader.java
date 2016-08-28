package com.example.pavl.tabstest;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiDialog;
import com.vk.sdk.api.model.VKApiGetDialogResponse;
import com.vk.sdk.api.model.VKList;

/**
 * Created by pavl on 16.08.2016.
 */
public class DialogLoader extends AsyncTaskLoader<VKList<VKApiDialog>> {

    private static final String TAG = DialogLoader.class.getName();

    public DialogLoader(Context context) {
        super(context);
        Log.i(TAG, "mister, DialogLoader()");
    }

    @Override
    protected void onStartLoading() {
        Log.i(TAG, "mister, onStartLoading()");
        super.onStartLoading();
        forceLoad();
    }

    @Override
    public VKList<VKApiDialog> loadInBackground() {
        //send request
        Log.i(TAG, "mister, loadInBackground()");

        //getId
        //getDialogs
        //getUsersPhoto + Name
        //getHistory

        VKRequest request = VKApi.messages().getDialogs(VKParameters.from(VKApiConst.COUNT, 10));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                VKApiGetDialogResponse dialogResponse = (VKApiGetDialogResponse) response.parsedModel;
                VKList<VKApiDialog> dialogs = dialogResponse.items;
//                return dialogs;
                Log.i(TAG, "mister, users().get() request success. response = " + response);
            }

            @Override
            public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                Log.i(TAG, "mister, users().get() request attempt failed");
            }

            @Override
            public void onError(VKError error) {
                Log.i(TAG, "mister, users().get() request error");
            }
        });

        return null;
    }


}
