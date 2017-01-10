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
package com.example.android.sunshine.sync;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.android.sunshine.shared_resources.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Simple IntentService subclass handling asynchronous request for Sunshine mobile app.
 */
public class MessageService extends IntentService {
    private static final String TAG = MessageService.class.getSimpleName();

    public static final String ACTION_LAUNCH_SUNSHINE =
            "com.example.android.sunshine.weather_provider.action.LAUNCH_SUNSHINE";
    public static final String ACTION_SYNC_SUNSHINE =
            "com.example.android.sunshine.weather_provider.action.SYNC_SUNSHINE";
    public static final String SUNSHINE_LAUNCHER_CAPABILITY_NAME = "sunshine_launch_app";
    public static final String SUNSHINE_SYNC_CAPABILITY_NAME = "sunshine_sync_app";

    public MessageService() {
        super(MessageService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            // Connect to Play Services and the Wearable API
            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Wearable.API)
                    .build();

            ConnectionResult connectionResult = googleApiClient.blockingConnect(
                    Constants.GOOGLE_API_CLIENT_TIMEOUT, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess() || !googleApiClient.isConnected()) {
                Log.e(TAG, String.format(Constants.GOOGLE_API_CONNECTION_ERROR,
                        connectionResult.getErrorCode()));
            }

            if (ACTION_LAUNCH_SUNSHINE.equals(action)) {
                // Detect capable nodes
                CapabilityApi.GetCapabilityResult result =
                        Wearable.CapabilityApi.getCapability(
                                googleApiClient, SUNSHINE_LAUNCHER_CAPABILITY_NAME,
                                CapabilityApi.FILTER_REACHABLE).await();

                Set<Node> connectedNodes = result.getCapability().getNodes();
                String bestNodeId = pickBestNodeId(connectedNodes);

                if (bestNodeId != null) {
                    Wearable.MessageApi.sendMessage(googleApiClient, bestNodeId,
                            Constants.LAUNCH_SUNSHINE_MESSAGE_PATH, null).setResultCallback(
                            new ResultCallback<MessageApi.SendMessageResult>() {
                                @Override
                                public void onResult(
                                        @NonNull MessageApi.SendMessageResult sendMessageResult) {
                                    if (!sendMessageResult.getStatus().isSuccess()) {
                                        Log.e(TAG, "onResult: Failed to send message to the node.");
                                    }
                                }
                            }
                    );
                } else {
                    // Unable to retrieve node with transcription capability
                    Log.e(TAG, "onHandleIntent: Unable to retrieve node with capability of " +
                            "launching Sunshine.");
                }
            }

            if (ACTION_SYNC_SUNSHINE.equals(action)) {
                // Detect capable nodes
                CapabilityApi.GetCapabilityResult result =
                        Wearable.CapabilityApi.getCapability(
                                googleApiClient, SUNSHINE_SYNC_CAPABILITY_NAME,
                                CapabilityApi.FILTER_REACHABLE).await();

                Set<Node> connectedNodes = result.getCapability().getNodes();
                String bestNodeId = pickBestNodeId(connectedNodes);

                if (bestNodeId != null) {
                    Wearable.MessageApi.sendMessage(googleApiClient, bestNodeId,
                            Constants.SYNC_SUNSHINE_MESSAGE_PATH, null).setResultCallback(
                            new ResultCallback<MessageApi.SendMessageResult>() {
                                @Override
                                public void onResult(
                                        @NonNull MessageApi.SendMessageResult sendMessageResult) {
                                    if (!sendMessageResult.getStatus().isSuccess()) {
                                        Log.e(TAG, "onResult: Failed to send message to the node.");
                                    }
                                }
                            }
                    );
                } else {
                    // Unable to retrieve node with transcription capability
                    Log.e(TAG, "onHandleIntent: Unable to retrieve node with capability of " +
                            "syncing data.");
                }
            }

            googleApiClient.disconnect();
        }
    }

    /**
     * Creates an intent to launch Sunshine on handheld device.
     * @param context Application's context
     * @return PendingIntent that launches Sunshine
     */
    public static PendingIntent getLaunchSunshineIntent(Context context) {
        Intent intent = new Intent(context, MessageService.class);
        intent.setAction(MessageService.ACTION_LAUNCH_SUNSHINE);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    /**
     * Helper method that sends request sync message.
     * @param context Application's context
     */
    public static void requestSyncIntent(Context context) {
        Intent intent = new Intent(context, MessageService.class);
        intent.setAction(MessageService.ACTION_SYNC_SUNSHINE);
        context.startService(intent);
    }

    /** Helper method to find best node to send message to
     * @param nodes Capable Nodes
     * @return Id of best node
     */
    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

}
