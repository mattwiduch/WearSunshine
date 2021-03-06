package com.example.android.sunshine.app.sync;

import android.content.Intent;

import com.example.android.sunshine.app.MainActivity;
import com.example.android.sunshine.shared_resources.Constants;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Simple listener service that receives messages from wearable device.
 */

public class SunshineWearListener extends WearableListenerService {
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // Launch Sunshine mobile app
        if (messageEvent.getPath().equals(Constants.LAUNCH_SUNSHINE_MESSAGE_PATH)) {
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
        // Sync Sunshine data
        if (messageEvent.getPath().equals(Constants.SYNC_SUNSHINE_MESSAGE_PATH)) {
            SunshineSyncAdapter.syncImmediately(getApplicationContext());
        }
    }
}
