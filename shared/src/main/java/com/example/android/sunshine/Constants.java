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
package com.example.android.sunshine;

/** Constants shared between app modules **/
public class Constants {

    private Constants() {};

    // Paths to weather data items in DataLayer API
    public static final String WEATHER_DATA_TEMP_PATH = "/weather_update/temperature";
    public static final String WEATHER_DATA_HUMIDITY_PATH = "/weather_update/humidity";
    public static final String WEATHER_DATA_SUMMARY_PATH = "/weather_update/summary";

    // Paths to messages sent through DataLayer API
    public static final String LAUNCH_SUNSHINE_MESSAGE_PATH = "/launch_sunshine";

    // Keys used to store weather data in DataLayer API
    public static final String HIGH_KEY = "com.example.android.sunshine.app.sync.key.high_temp";
    public static final String LOW_KEY = "com.example.android.sunshine.app.sync.key.low_temp";
    public static final String HUMIDITY_KEY = "com.example.android.sunshine.app.sync.key.humidity";
    public static final String SUMMARY_KEY = "com.example.android.sunshine.app.sync.key.summary";

    // GoogleApiClient connection timeout (10 seconds)
    public static final int GOOGLE_API_CLIENT_TIMEOUT = 10;

    // GoogleApiClient error messages
    public static final String GOOGLE_API_CONNECTION_ERROR =
            "Failed to connect to GoogleApiClient (error code = %d)";
    public static final String GOOGLE_API_CLIENT_ERROR =
            "Error sending data using DataApi (error code = %d)";
}
