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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.util.Log;

import com.example.android.sunshine.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Sunshine Watch Face Complication data provider for current weather summary complication.
 */
public class SummaryProviderService extends ComplicationProviderService {

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
        Log.d(TAG, "onComplicationUpdate(): " + complicationId);

        // Retrieve summary data in background thread
        new SummaryProviderService.FetchWeatherAsyncTask(this, complicationId, dataType,
                complicationManager).execute();
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
            AsyncTask<Void, Void, Bitmap> {

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
        protected Bitmap doInBackground(Void... params) {
            Bitmap weatherIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

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
            // Create Uri for humidity data
            Uri weatherDataUri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME)
                    .authority(nodeResult.getNode().getId())
                    .path(Constants.WEATHER_DATA_SUMMARY_PATH).build();

            DataApi.DataItemResult dataItemResult =
                    Wearable.DataApi.getDataItem(googleApiClient, weatherDataUri).await();

            if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItemResult.getDataItem());
                Asset summary = dataMapItem.getDataMap().getAsset(Constants.SUMMARY_KEY);
                if (summary != null) {
                    // convert asset into a file descriptor and block until it's ready
                    InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                            googleApiClient, summary).await().getInputStream();

                    if (assetInputStream != null) {
                        weatherIcon = BitmapFactory.decodeStream(assetInputStream);
                    } else {
                        Log.w(TAG, "Requested an unknown Asset.");
                    }
                }
            }

            googleApiClient.disconnect();

            return weatherIcon;
        }

        @Override
        protected void onPostExecute(Bitmap icon) {
            ComplicationData complicationData = null;

            switch (mDataType) {
                case ComplicationData.TYPE_SMALL_IMAGE:
                    Log.d(TAG, "TYPE_SMALL_IMAGE");
                    complicationData = new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                            .setImageStyle(ComplicationData.IMAGE_STYLE_ICON)
                            .setSmallImage(Icon.createWithBitmap(icon))
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
}
