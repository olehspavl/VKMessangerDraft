package com.example.pavl.tabstest;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.pavl.tabstest.data.DataHandler;
import com.example.pavl.tabstest.data.HistoryDataChangedNotifier;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKApiUser;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;

/**
 * A fragment representing a single Dialog detail screen.
 * This fragment is either contained in a {@link DialogListActivity}
 * in two-pane mode (on tablets) or a {@link DialogDetailActivity}
 * on handsets.
 */
public class DialogDetailFragment extends Fragment implements Observer {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "user_id";
    private static final String TAG = DialogDetailFragment.class.getName();
    private static final int UPDATE_MSG = 1;

    /**
     * The dummy content this fragment is presenting.
     */
//    private DummyContent.DummyItem mHistory;
//    private VKApiDialog mHistory;
    private List<VKApiMessage> mHistory;
    private VKApiUser mOwner;
    private VKApiUser mOpponent;// TODO getItFromFragmentArgs

    private Handler mUpdater;
    private RecyclerView mRecyclerView;
    private Button mSendBtn;
    private EditText mMsgEditText;

    private Timer mTimer = new Timer();//TODO change it to service
    private RESTHandler.GetHistoryTimerTask mGetHistoryTask;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DialogDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            int opponentId = getArguments().getInt(ARG_ITEM_ID);

            mHistory = HistoryDataChangedNotifier.getInstance().getHistory(String.valueOf(opponentId));
            mOwner = DataHandler.getInstance().getOwnerUser();
            mOpponent = DataHandler.getInstance().getUser(opponentId);

            mGetHistoryTask = new RESTHandler.GetHistoryTimerTask(opponentId);
//            Activity activity = this.getActivity();
//            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
//            if (appBarLayout != null) {
//                appBarLayout.setTitle("Title"/*String.valueOf(mHistory.message.user_id)*/);//TODO setUser name & last visit date
//            }

            initUpdateViewHandler();
        }
    }

    private void initUpdateViewHandler() {
        mUpdater = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UPDATE_MSG:
                        setupView();
                }
                super.handleMessage(msg);
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        HistoryDataChangedNotifier.getInstance().deleteObserver(this);
        Log.i(TAG, "mister, onDestroyView details" + testObjectName(this));
    }

    @Override
    public void onPause() {
        super.onPause();
        mTimer.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTimer.schedule(mGetHistoryTask, 15000, 15000);//use exponent time difference according to existing of new messages
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_detail_list, container, false);

        mRecyclerView = (RecyclerView)rootView.findViewById(R.id.detailListRV);

        LinearLayoutManager lManager = new LinearLayoutManager(getContext());
        lManager.setReverseLayout(true);
        mRecyclerView.setLayoutManager(lManager);

        mMsgEditText = (EditText)rootView.findViewById(R.id.detailsMsgEditText);
        mMsgEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    sendMsg();
                    return true;
                }
                return false;
            }
        });
        mSendBtn = (Button)rootView.findViewById(R.id.detailsSendBtn);
        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMsg();
            }
        });

        HistoryDataChangedNotifier.getInstance().addObserver(this);
        Log.i(TAG, "mister, onCreateView details" + testObjectName(this));

        updateView();
        return rootView;
    }

    private void sendMsg() {
        RESTHandler.getInstance().sendMsg(mOpponent.getId(), mMsgEditText.getText().toString());
        mMsgEditText.getText().clear();
    }

    //TODO remove method
    public static String testObjectName(Object o) {
        return o.getClass().getName() + '@' + Integer.toHexString(o.hashCode());
    }

    //TODO rename the method
    public void updateView() {
        Log.i(TAG, "mister, updateView details" + testObjectName(this));
        RESTHandler.getInstance().requestForHistory(getArguments().getInt(ARG_ITEM_ID));
    }

    @Override
    public void update(Observable observable, Object data) {
        Log.i(TAG, "mister, update details" + testObjectName(this));
        int opponentId = getArguments().getInt(ARG_ITEM_ID);
        mHistory = HistoryDataChangedNotifier.getInstance().getHistory(String.valueOf(opponentId));
        mOwner = DataHandler.getInstance().getOwnerUser();
        mOpponent = DataHandler.getInstance().getUser(opponentId);

        mTimer.cancel();
        mGetHistoryTask = new RESTHandler.GetHistoryTimerTask(opponentId);
        mTimer = new Timer();
        mTimer.schedule(mGetHistoryTask, 15000, 15000);

        if (argsNotNull()) mUpdater.sendEmptyMessage(UPDATE_MSG);
    }

    private boolean argsNotNull() {
        return mHistory != null && mOwner != null && mRecyclerView != null;
    }

    private void setupView() {
        Log.i(TAG, "mister, setupView details" + testObjectName(this));

        HistoryAdapter hAdapter = new HistoryAdapter(mHistory, mOwner, mOpponent);
        mRecyclerView.setAdapter(hAdapter);
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        private VKApiUser mOwner;
        private VKApiUser mOpponent;
        private List<VKApiMessage> mHistory;

        public HistoryAdapter(List<VKApiMessage> history, VKApiUser opponent, VKApiUser owner) {
            mHistory = history;
            mOpponent = opponent;
            mOwner = owner;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dialog_detail_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mHistory.get(position);

            VKApiUser author = holder.mItem.out ? mOpponent : mOwner;

            holder.mNameTV.setText(author.first_name + " " + author.last_name);
            Picasso.with(holder.mView.getContext()).load(author.photo_50).into(holder.mIconIV, new Callback() {
                @Override
                public void onSuccess() {
                    holder.mIconIV.setVisibility(View.VISIBLE);
                    holder.mIconLoadProgressBar.setVisibility(View.GONE);
                }

                @Override
                public void onError() {
                }
            });
            holder.mContentView.setText(holder.mItem.body);
            holder.mDateView.setText(parseDate(holder.mItem.date));
        }

        //TODO make it static in utils
        private String parseDate(long time) {
            return new SimpleDateFormat("dd hh:mm:ss").format(time).toString();
        }

        @Override
        public int getItemCount() {
            return mHistory.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final ImageView mIconIV;
            public final TextView mNameTV;
            public final TextView mContentView;
            public final TextView mDateView;
            public final ProgressBar mIconLoadProgressBar;
            public VKApiMessage mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;

                mIconIV = (ImageView) view.findViewById(R.id.detailsItemIcon);
                mNameTV = (TextView) view.findViewById(R.id.detailsItemNameTV);
                mContentView = (TextView) view.findViewById(R.id.detailsItemMessageTV);
                mDateView = (TextView) view.findViewById(R.id.detailsItemDateTV);
                mIconLoadProgressBar = (ProgressBar) view.findViewById(R.id.detailsItemProgressBar);
            }
        }
    }
}
