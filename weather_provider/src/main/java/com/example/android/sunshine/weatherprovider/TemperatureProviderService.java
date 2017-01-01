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
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;

import com.example.android.sunshine.shared_resources.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Sunshine Watch Face Complication data provider for high/low temperature complication.
 */
public class TemperatureProviderService extends ComplicationProviderService {

    private static final String TAG = "WeatherProvider";

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
        MessageService.requestSyncIntent(getApplicationContext());
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
        Log.d(TAG, "onTemperatureComplicationUpdate(): " + complicationId);

        // Retrieve temperature data in background thread
        new FetchWeatherAsyncTask(this, complicationId, dataType, complicationManager).execute();
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
            AsyncTask<Void, Void, DataWrapper> {

        private Context mContext;
        private int mComplicationId;
        private int mDataType;
        private ComplicationManager mComplicationManager;

        public FetchWeatherAsyncTask(Context context, int complicationId,
                                     int dataType, ComplicationManager complicationManager) {
            mContext = context;
            mComplicationId = complicationId;
            mDataType = dataType;
            mComplicationManager = complicationManager;
        }

        @Override
        protected DataWrapper doInBackground(Void... params) {
            String low = getString(R.string.complications_temperature_label);
            String high = getString(R.string.complications_no_data);

            // Connect to Play Services and the Wearable API
            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Wearable.API)
                    .build();

            ConnectionResult connectionResult = googleApiClient.blockingConnect(
                    Constants.GOOGLE_API_CLIENT_TIMEOUT, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess() || !googleApiClient.isConnected()) {
                Log.e(TAG, String.format(Constants.GOOGLE_API_CONNECTION_ERROR,
                        connectionResult.getErrorCode()));
            }

            // Get wearable's node it
            NodeApi.GetLocalNodeResult nodeResult = Wearable.NodeApi.getLocalNode(googleApiClient).await();
            // Create Uri for temperature data
            Uri weatherDataUri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME)
                    .authority(nodeResult.getNode().getId())
                    .path(Constants.WEATHER_DATA_TEMP_PATH).build();

            DataApi.DataItemResult dataItemResult =
                    Wearable.DataApi.getDataItem(googleApiClient, weatherDataUri).await();

            if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItemResult.getDataItem());
                low = dataMapItem.getDataMap().getString(Constants.LOW_KEY);
                high = dataMapItem.getDataMap().getString(Constants.HIGH_KEY);
            }

            googleApiClient.disconnect();

            return new DataWrapper(low, high);
        }

        @Override
        protected void onPostExecute(DataWrapper data) {
            ComplicationData complicationData = null;

            switch (mDataType) {
                case ComplicationData.TYPE_SHORT_TEXT:
                    Log.d(TAG, "TYPE_SHORT_TEXT");
                    complicationData = new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setShortTitle(ComplicationText.plainText(data.mLow))
                            .setShortText(ComplicationText.plainText(data.mHigh))
                            .setTapAction(MessageService.getLaunchSunshineIntent(mContext))
                            .build();
                    break;
                default:
                    if (Log.isLoggable(TAG, Log.WARN)) {
                        Log.w(TAG, "Unexpected temperature complication type " + mDataType);
                    }
            }

            if (complicationData != null) {
                mComplicationManager.updateComplicationData(mComplicationId, complicationData);
            }
        }
    }

    /** Helper class used to pass retrieved data to OnPostExecute **/
    private class DataWrapper {
        private String mLow;
        private String mHigh;

        DataWrapper(String low, String high) {
            mLow = low;
            mHigh = high;
        }
    }
}
