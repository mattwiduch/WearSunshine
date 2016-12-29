/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.weatherprovider;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Sunshine Watch Face Complication data provider for humidity percentage complication.
 */
public class HumidityProviderService extends ComplicationProviderService {

    private static final String TAG = "WeatherProvider";
    private static final String HUMIDITY_KEY = "com.example.android.sunshine.app.sync.key.humidity";

    /*
     * Called when a complication has been activated. The method is for any one-time
     * (per complication) set-up.
     *
     * You can continue sending data for the active complicationId until onComplicationDeactivated()
     * is called.
     */
    @Override
    public void onComplicationActivated(
            int complicationId, int dataType, ComplicationManager complicationManager) {
        Log.d(TAG, "onComplicationActivated(): " + complicationId);
        super.onComplicationActivated(complicationId, dataType, complicationManager);
    }

    /*
     * Called when the complication needs updated data from your provider. There are four scenarios
     * when this will happen:
     *
     *   1. An active watch face complication is changed to use this provider
     *   2. A complication using this provider becomes active
     *   3. The period of time you specified in the manifest has elapsed (UPDATE_PERIOD_SECONDS)
     *   4. You triggered an update from your own class via the
     *       ProviderUpdateRequester.requestUpdate() method.
     */
    @Override
    public void onComplicationUpdate(
            int complicationId, int dataType, ComplicationManager complicationManager) {
        Log.d(TAG, "onComplicationUpdate(): " + complicationId);

        // Create Uri for humidity data
        Uri weatherDataUri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME)
                .authority("*").path("/weather_data/humidity").build();
        // Retrieve humidity data in background thread
        if (weatherDataUri != null) {
            new FetchWeatherAsyncTask(this, weatherDataUri, complicationId, dataType,
                    complicationManager).execute();
        }
    }

    /*
     * Called when the complication has been deactivated. If you are updating the complication
     * manager outside of this class with updates, you will want to update your class to stop.
     */
    @Override
    public void onComplicationDeactivated(int complicationId) {
        Log.d(TAG, "onComplicationDeactivated(): " + complicationId);
        super.onComplicationDeactivated(complicationId);
    }

    /**
     * A background task to load the weather data via Wear DataApi.
     */
    private class FetchWeatherAsyncTask extends
            AsyncTask<Void, Void, Double> {

        private Context mContext;
        private Uri mWeatherDataUri;
        private int mComplicationId;
        private int mDataType;
        private ComplicationManager mComplicationManager;

        public FetchWeatherAsyncTask(Context context, Uri weatherDataUri, int complicationId,
                                     int dataType, ComplicationManager complicationManager) {
            mContext = context;
            mWeatherDataUri = weatherDataUri;
            mComplicationId = complicationId;
            mDataType = dataType;
            mComplicationManager = complicationManager;
        }

        @Override
        protected Double doInBackground(Void... params) {
            Double humidity = 0d;

            // Connect to Play Services and the Wearable API
            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Wearable.API)
                    .build();

            ConnectionResult connectionResult = googleApiClient.blockingConnect(
                    10, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess() || !googleApiClient.isConnected()) {
                Log.e(TAG, String.format("Failed to connect to GoogleApiClient (error code = %d)",
                        connectionResult.getErrorCode()));
            }

            DataApi.DataItemResult dataItemResult =
                    Wearable.DataApi.getDataItem(googleApiClient, mWeatherDataUri).await();

            if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItemResult.getDataItem());
                humidity = dataMapItem.getDataMap().getDouble(HUMIDITY_KEY);
            }

            googleApiClient.disconnect();

            return humidity;
        }
    }
}
