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

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

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
            if (event.getType() == DataEvent.TYPE_CHANGED
                    && event.getDataItem() != null
                    && ("/weather_update").equals(event.getDataItem().getUri().getPath())) {
                // Request complications update only when valid weather data is received
                ComponentName componentName =
                        new ComponentName(getApplicationContext(), WeatherProviderService.class);

                ProviderUpdateRequester providerUpdateRequester =
                        new ProviderUpdateRequester(getApplicationContext(), componentName);

                providerUpdateRequester.requestUpdateAll();
            }
        }
    }
}
