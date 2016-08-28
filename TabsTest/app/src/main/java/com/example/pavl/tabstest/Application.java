package com.example.pavl.tabstest;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.example.pavl.tabstest.data.DataHandler;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKAccessTokenTracker;
import com.vk.sdk.VKSdk;

/**
 * Created by pavl on 17.08.2016.
 */
public class Application extends android.app.Application {
    private static final String TAG = Application.class.getName();

    VKAccessTokenTracker tokenTracker = new VKAccessTokenTracker() {
        @Override
        public void onVKAccessTokenChanged(@Nullable VKAccessToken oldToken, @Nullable VKAccessToken newToken) {
            if (newToken == null) {
                Toast.makeText(Application.this, "AccessToken invalidated", Toast.LENGTH_LONG).show();

                DataHandler.getInstance().setOwnerId(Integer.decode(newToken.userId));

                Intent intent = new Intent(Application.this, DialogListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                Log.i(TAG, "Mister, VKAccessToken is invalid");
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        tokenTracker.startTracking();

        VKSdk.initialize(this);

        DataHandler.getInstance().setOwnerId(Integer.decode(VKAccessToken.currentToken().userId));

    }

    //using for getting fingerprint
    //private void logFingerprint() {
    //    String[] fingerprints = VKUtil.getCertificateFingerprint(this, this.getPackageName());
    //    Log.i(TAG, "Mister, fingerprints = " + fingerprints);
    //}

}