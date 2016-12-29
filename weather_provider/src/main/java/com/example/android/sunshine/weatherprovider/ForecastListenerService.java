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

import android.content.ComponentName;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

import com.example.android.sunshine.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

import static com.example.android.sunshine.Constants.HUMIDITY_KEY;
import static com.example.android.sunshine.Constants.WEATHER_DATA_HUMIDITY_PATH;


/**
 * Simple wearable listener service that requests complications data update whenever
 * weather data in the Data Layer API is changed.
 */

public class ForecastListenerService extends WearableListenerService {
    private static final String TAG = WearableListenerService.class.getSimpleName();

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "onDataChanged: " + dataEventBuffer);

        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem() != null) {
                if ((Constants.WEATHER_DATA_TEMP_PATH).equals(event.getDataItem().getUri().getPath())) {
                    // Request complications update only when valid weather data is received
                    ComponentName componentName =
                            new ComponentName(getApplicationContext(), TemperatureProviderService.class);

                    ProviderUpdateRequester providerUpdateRequester =
                            new ProviderUpdateRequester(getApplicationContext(), componentName);

                    providerUpdateRequester.requestUpdateAll();
                }
                if ((Constants.WEATHER_DATA_HUMIDITY_PATH).equals(event.getDataItem().getUri().getPath())) {
                    // Get Data
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Double humidity = dataMapItem.getDataMap().getDouble(Constants.HUMIDITY_KEY);

                    // Connect to Google API Client
                    GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                            .addApi(Wearable.API)
                            .build();

                    ConnectionResult connectionResult = googleApiClient.blockingConnect(
                            10, TimeUnit.SECONDS);

                    if (!connectionResult.isSuccess() || !googleApiClient.isConnected()) {
                        Log.e(TAG, String.format(Constants.GOOGLE_API_CONNECTION_ERROR,
                                connectionResult.getErrorCode()));
                        return;
                    }

                    // Update data locally
                    PutDataMapRequest humidityDataMap = PutDataMapRequest.create(WEATHER_DATA_HUMIDITY_PATH);
                    humidityDataMap.getDataMap().putDouble(HUMIDITY_KEY, humidity);
                    PutDataRequest humidityRequest = humidityDataMap.asPutDataRequest();
                    humidityRequest.setUrgent();
                    DataApi.DataItemResult humidityResult =
                            Wearable.DataApi.putDataItem(googleApiClient, humidityRequest).await();

                    if (!humidityResult.getStatus().isSuccess()) {
                        Log.e(TAG, String.format(Constants.GOOGLE_API_CLIENT_ERROR,
                                humidityResult.getStatus().getStatusCode()));
                    }

                    // Disconnect
                    googleApiClient.disconnect();

                    // Request complications update only when valid weather data is received
                    ComponentName componentName =
                            new ComponentName(getApplicationContext(), HumidityProviderService.class);

                    ProviderUpdateRequester providerUpdateRequester =
                            new ProviderUpdateRequester(getApplicationContext(), componentName);

                    providerUpdateRequester.requestUpdateAll();
                }
                if ((Constants.WEATHER_DATA_SUMMARY_PATH).equals(event.getDataItem().getUri().getPath())) {
                    // Request complications update only when valid weather data is received
                    ComponentName componentName =
                            new ComponentName(getApplicationContext(), SummaryProviderService.class);

                    ProviderUpdateRequester providerUpdateRequester =
                            new ProviderUpdateRequester(getApplicationContext(), componentName);

                    providerUpdateRequester.requestUpdateAll();
                }

            }
        }
    }
}
