package com.example.pavl.tabstest;

import android.os.AsyncTask;
import android.util.Log;

import com.example.pavl.tabstest.data.DataHandler;
import com.example.pavl.tabstest.data.DialogsDataChangedNotifier;
import com.example.pavl.tabstest.data.HistoryDataChangedNotifier;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiDialog;
import com.vk.sdk.api.model.VKApiGetDialogResponse;
import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKList;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by pavl on 21.08.2016.
 */
public class RESTHandler {
    private static final String TAG = RESTHandler.class.getName();
    private static RESTHandler ourInstance = new RESTHandler();
//    private Handler mResultHandler;

    public static RESTHandler getInstance() {
        return ourInstance;
    }

    private RESTHandler() {
    }

//    public void setResultHandler(Handler resultHandler) {
//        this.mResultHandler = resultHandler;
//    }

    public void requestCurrentUserInfo() {
        VKRequest request = VKApi.users().get(VKParameters.from(VKApiConst.FIELDS, "photo_50"));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                VKList<VKApiUser> users = (VKList<VKApiUser>) response.parsedModel;
                new OwnerUserDataUpdateTask().execute(users.get(0));
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
    }

    public void requestForDialogs() {

        VKRequest request = VKApi.messages().getDialogs(VKParameters.from(VKApiConst.COUNT, 10));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                VKApiGetDialogResponse dialogResponse = (VKApiGetDialogResponse) response.parsedModel;
                VKList<VKApiDialog> dialogs = dialogResponse.items;
                requestForUsers(dialogs);

                Log.i(TAG, "mister, messages().getDialogs request success. response = " + response);
            }

            @Override
            public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                Log.i(TAG, "mister, messages().getDialogs request attempt failed");
            }

            @Override
            public void onError(VKError error) {
                Log.i(TAG, "mister, messages().getDialogs request error");
            }
        });
    }

    public void requestForUsers(final VKList<VKApiDialog> dialogs) {

        List<Integer> users_ids = new ArrayList<>();
        for (VKApiDialog dialog : dialogs) {
            users_ids.add(dialog.message.user_id);
        }

        VKRequest request = VKApi.users().get(VKParameters.from(VKApiConst.USER_IDS, convIdsToReqParam(users_ids),
                VKApiConst.FIELDS, "photo_50"));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                VKList<VKApiUser> users = (VKList<VKApiUser>) response.parsedModel;
                new DialogsDataSaveTask().execute(dialogs, users);
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
    }

    public void requestForHistory(final int user_id) {
        VKRequest request = new VKRequest("messages.getHistory", VKParameters.from(VKApiConst.REV, 0, VKApiConst.COUNT, 40, VKApiConst.USER_ID, String.valueOf(user_id)));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                List<VKApiMessage> messages = new ArrayList<>();
                try {
                    JSONArray items = response.json.getJSONObject("response").getJSONArray("items");
                    for (int idx = 0; idx < items.length(); ++idx) {
                        messages.add(new VKApiMessage(items.getJSONObject(idx)));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                Collections.reverse(messages);
                new HistoryDataSaveTask(String.valueOf(user_id)).execute(messages);
                Log.i(TAG, "mister, messages.getHistory request success. response = " + response);
            }
        });
    }

    private String convIdsToReqParam(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (Integer item : list) {
            sb.append(item).append(", ");
        }
        String s = sb.toString();
        s = s.substring(0, s.length() - 2);
        return s;
    }

    public void sendMsg(final int recipientId, String message) {
        VKRequest request = new VKRequest("messages.send", VKParameters.from(VKApiConst.USER_ID, recipientId, VKApiConst.MESSAGE, message));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                try {
                    int msgId = response.json.getInt("response");
                    addMsgToDialog(recipientId, msgId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "mister, sendMsg success");
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);
                if (error.errorCode == 900) {
                    Log.i(TAG, "mister, sendMsg error : you trying to send msg from the Black List");
                } else if (error.errorCode == 902) {
                    Log.i(TAG, "mister, sendMsg error : you cant send msg because of user privacy settings");
                }
            }
        });
    }

    private void addMsgToDialog(final int recipientId, int msgId) {
        VKRequest request = new VKRequest("messages.getById", VKParameters.from("message_ids", msgId));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                List<VKApiMessage> list = new ArrayList<>(1);
                try {
                    JSONArray items = response.json.getJSONObject("response").getJSONArray("items");
                    for (int idx = 0; idx < items.length(); ++idx) {
                        list.add(new VKApiMessage(items.getJSONObject(idx)));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                new HistoryDataMsgAddTask(recipientId).execute(list);
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);
                if (error.errorCode == 900) {
                    Log.i(TAG, "mister, sendMsg error : you trying to send msg from the Black List");
                } else if (error.errorCode == 902) {
                    Log.i(TAG, "mister, sendMsg error : you cant send msg because of user privacy settings");
                }
            }
        });
    }

    //update data timers
    public static class GetDialogsTimerTask extends TimerTask {
        @Override
        public void run() {
            RESTHandler.getInstance().requestForDialogs();
        }
    }

    public static class GetHistoryTimerTask extends TimerTask {
        int mOpponentId;

        public GetHistoryTimerTask(int opponentId) {
            mOpponentId = opponentId;
        }

        @Override
        public void run() {
            RESTHandler.getInstance().requestForHistory(mOpponentId);
        }
    }

    //These acynks is not required
    public class HistoryDataMsgAddTask extends AsyncTask<List, Void, Void> {

        private int mUserId;

        public HistoryDataMsgAddTask(int userId) {
            mUserId = userId;
        }

        @Override
        protected Void doInBackground(List... params) {
            HistoryDataChangedNotifier.getInstance().addToHistory(mUserId, params[0]);
            return null;
        }
    }

    public class DialogsDataSaveTask extends AsyncTask<List, Void, Void> {

        @Override
        protected Void doInBackground(List... params) {
            DialogsDataChangedNotifier.getInstance().saveDialogsAndUsers(params[0], params[1]);
            return null;
        }
    }

    public class OwnerUserDataUpdateTask extends AsyncTask<VKApiUser, Void, Void> {

        @Override
        protected Void doInBackground(VKApiUser... params) {
            DataHandler.getInstance().saveOwnerUser(params[0]);
            return null;
        }
    }

    public class HistoryDataSaveTask extends AsyncTask<List, Void, Void> {

        private String mUserId;

        public HistoryDataSaveTask(String userId) {
            mUserId = userId;
        }

        @Override
        protected Void doInBackground(List... params) {
            HistoryDataChangedNotifier.getInstance().saveHistory(mUserId, params[0]);
            return null;
        }
    }
}
