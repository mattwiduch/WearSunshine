/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Simple IntentService subclass handling asynchronous request for Sunshine mobile app.
 */
public class MessageService extends IntentService {
    public static final String ACTION_LAUNCH_SUNSHINE =
            "com.example.android.sunshine.weather_provider.action.LAUNCH_SUNSHINE";

    public MessageService() {
        super(MessageService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_LAUNCH_SUNSHINE.equals(action)) {

            }
        }
    }

    /**
     * Creates an intent to launch Sunshine on handheld device.
     * @param context Application's context
     * @return PendingIntent that launches Sunshine
     */
    static PendingIntent getLaunchSunshineIntent(Context context) {
        Intent intent = new Intent(context, MessageService.class);
        intent.setAction(MessageService.ACTION_LAUNCH_SUNSHINE);
        return PendingIntent.getService(context, 0, intent, 0);
    }

}
