package com.example.pavl.tabstest;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pavl.tabstest.data.DialogsDataChangedNotifier;
import com.example.pavl.tabstest.data.MySQLiteHelper;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.model.VKApiDialog;
import com.vk.sdk.api.model.VKApiUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;

/**
 * An activity representing a list of Dialogs. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link DialogDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class DialogListActivity extends AppCompatActivity
        implements Observer {

    private static final int MSG_UPDATE_UI = 0;
    private static final String TAG = DialogListActivity.class.getName();
    private static final String[] SCOPE = new String[] {
            VKScope.FRIENDS,
            VKScope.MESSAGES
    };

    private boolean mTwoPane;

    private Handler mViewUpdateHandler;

    private Timer mTimer;//TODO change it to service

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_list);

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Fix it, please.", Toast.LENGTH_LONG).show();
        }

        //init UI
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        if (findViewById(R.id.dialog_detail_container) != null) {
            mTwoPane = true;
        }

        int selectedItem = mTwoPane ? 0 : -1;

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.dialog_list);
        assert recyclerView != null;
        SimpleItemRecyclerViewAdapter adapter = new SimpleItemRecyclerViewAdapter(
                new ArrayList<VKApiDialog>(), new ArrayList<VKApiUser>(), selectedItem);
        recyclerView.setAdapter(adapter);

        initUpdateHandler();

        //get data
        DialogsDataChangedNotifier.getInstance().addObserver(this);
        new MySQLiteHelper.TaskGetDialogsAndUsersFromDBAsync().execute(this);

        if (!VKSdk.isLoggedIn()) {
            VKSdk.login(this, SCOPE);
        }

        RESTHandler.getInstance().requestCurrentUserInfo();
        RESTHandler.getInstance().requestForDialogs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DialogsDataChangedNotifier.getInstance().addObserver(this);
        mTimer = new Timer();
        RESTHandler.GetDialogsTimerTask getDialogTask = new RESTHandler.GetDialogsTimerTask();
        mTimer.schedule(getDialogTask, 30000, 30000);//use exponent time difference according to existing of new messages
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTimer.cancel();
        new MySQLiteHelper.TaskSetDialogsAndUsersToDBAsync().execute(this);
        DialogsDataChangedNotifier.getInstance().deleteObserver(this);
    }

    //Methods from Observer interface
    @Override
    public void update(Observable observable, Object o) {
        Log.i(TAG, "mister, update mainList" + DialogDetailFragment.testObjectName(this));
        mViewUpdateHandler.sendEmptyMessage(MSG_UPDATE_UI);
    }

    private int getSelectedDialogUserId() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.dialog_list);
        SimpleItemRecyclerViewAdapter adapter = (SimpleItemRecyclerViewAdapter) recyclerView.getAdapter();
        return adapter.getSelectedUserId();
    }

    private void initUpdateHandler() {
        mViewUpdateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.i(TAG, "mister, handleMessage mainList handler" + DialogDetailFragment.testObjectName(this));
                switch (msg.what) {
                    case MSG_UPDATE_UI:
                        updateDialogList();
                        //TODO use activity callback for communicate with other fragment
                        if (mTwoPane) {
                            int id = getSelectedDialogUserId();
                            if (id == -1) return;
                            Bundle arg = new Bundle();
                            arg.putInt(DialogDetailFragment.ARG_ITEM_ID, id);

                            DialogDetailFragment fragment = (DialogDetailFragment) getSupportFragmentManager()
                                    .findFragmentById(R.id.dialog_detail_container);

                            if (fragment != null && fragment.isInLayout()) {
                                fragment.setArguments(arg);
                            } else {
                                fragment = new DialogDetailFragment();
                                fragment.setArguments(arg);
                                getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.dialog_detail_container, fragment)
                                        .commit();
                            }
                            fragment.updateView();
                        }
                        break;
                }
            }
        };
    }

    private void updateDialogList() {
        //getData
        List<VKApiDialog> dialogs = DialogsDataChangedNotifier.getInstance().getDialogList();//TODO use async
        List<VKApiUser> users = DialogsDataChangedNotifier.getInstance().getUserList();
        //setAdapter
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.dialog_list);
        SimpleItemRecyclerViewAdapter adapter = (SimpleItemRecyclerViewAdapter) recyclerView.getAdapter();
        adapter.resetData(dialogs, users);
    }

    //vk auth methods
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
            }
            @Override
            public void onError(VKError error) {
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<VKApiDialog> mDialogs;
        private final List<VKApiUser> mUsers;
        private int mSelectedItemIdx;

        public SimpleItemRecyclerViewAdapter(List<VKApiDialog> dialogs, List<VKApiUser> users) {
            this(dialogs, users, -1);
        }

        public SimpleItemRecyclerViewAdapter(List<VKApiDialog> dialogs, List<VKApiUser> users, int selectedItemIdx) {
            mDialogs = dialogs;
            mUsers = users;
            mSelectedItemIdx = selectedItemIdx;
        }

        public void resetData(List<VKApiDialog> dialogs, List<VKApiUser> user) {
            mDialogs.clear();
            mDialogs.addAll(dialogs);

            mUsers.clear();
            mUsers.addAll(user);

            notifyDataSetChanged();
        }

        public void setSelectedItemIdx(int idx) {
            mSelectedItemIdx = idx;
            notifyDataSetChanged();
        }

        public int getSelectedUserId() {
            if (mSelectedItemIdx == -1 || mUsers.size() == 0) return -1;
            else return mUsers.get(mSelectedItemIdx).getId();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dialog_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            holder.mItem = mDialogs.get(position);
            String idText = mUsers.get(position).first_name + " " + mUsers.get(position).last_name;
            holder.mIdView.setText(idText);
            holder.mContentView.setText(mDialogs.get(position).message.body);
            holder.mDateView.setText(parseDate(mDialogs.get(position).message.date));
            Picasso.with(holder.mView.getContext()).load(mUsers.get(position).photo_50).into(holder.mIconView, new Callback() {
                @Override
                public void onSuccess() {
                    holder.mIconView.setVisibility(View.VISIBLE);
                    holder.mIconLoadProgressBar.setVisibility(View.GONE);
                }

                @Override
                public void onError() {
                }
            });

            holder.mView.setSelected(position == mSelectedItemIdx);
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "mister, onClick mainList");
                    //TODO use activity callback for communicate with other fragment
                    if (mTwoPane) {
                        setSelectedItemIdx(position);

                        Bundle arg = new Bundle();
                        arg.putInt(DialogDetailFragment.ARG_ITEM_ID, holder.mItem.message.user_id);

                        DialogDetailFragment fragment = (DialogDetailFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.dialog_detail_container);

                        if (fragment != null && fragment.isInLayout()) {
                            fragment.setArguments(arg);
                        } else {
                            fragment = new DialogDetailFragment();
                            fragment.setArguments(arg);
                            getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.dialog_detail_container, fragment)
                                    .commit();
                        }
                        fragment.updateView();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, DialogDetailActivity.class);
                        intent.putExtra(DialogDetailFragment.ARG_ITEM_ID, holder.mItem.message.user_id);

                        context.startActivity(intent);
                    }
                }
            });
        }

        private String parseDate(long time) {
            return new SimpleDateFormat("dd hh:mm:ss").format(time).toString();
        }

        @Override
        public int getItemCount() {
            return mDialogs.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final ImageView mIconView;
            public final TextView mIdView;
            public final TextView mContentView;
            public final TextView mDateView;
            public final ProgressBar mIconLoadProgressBar;
            public VKApiDialog mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;

                mIconView = (ImageView) view.findViewById(R.id.dialogListIcon);
                mIdView = (TextView) view.findViewById(R.id.dialogListName);
                mContentView = (TextView) view.findViewById(R.id.dialogListLastMessage);
                mDateView = (TextView) view.findViewById(R.id.dialogsListMessageDate);
                mIconLoadProgressBar = (ProgressBar) view.findViewById(R.id.dialogsListProgressBar);
            }
        }
    }
}
