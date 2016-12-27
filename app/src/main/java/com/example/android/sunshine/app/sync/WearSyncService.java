package com.example.android.sunshine.app.sync;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

/**
 * Created by mateusz on 27/12/16.
 */

public class WearSyncService extends IntentService {

    private static final String TAG = WearSyncService.class.getSimpleName();

    /**
     * Default constructor.
     */
    public WearSyncService() {
        super(WearSyncService.class.getSimpleName());
    }

    /**
     * Static method used to start the service.
     * @param context Application's context
     */
    public static void startService(Context context) {
        Intent service = new Intent(context, WearSyncService.class);
        context.startService(service);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }
}
