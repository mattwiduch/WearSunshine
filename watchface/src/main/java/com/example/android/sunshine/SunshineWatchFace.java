/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static android.support.wearable.watchface.WatchFaceStyle.PROTECT_HOTWORD_INDICATOR;
import static android.support.wearable.watchface.WatchFaceStyle.PROTECT_STATUS_BAR;
import static com.example.android.sunshine.ComplicationsHelper.BOTTOM_DIAL_COMPLICATION;
import static com.example.android.sunshine.ComplicationsHelper.COMPLICATION_IDS;
import static com.example.android.sunshine.ComplicationsHelper.COMPLICATION_SUPPORTED_TYPES;
import static com.example.android.sunshine.ComplicationsHelper.LEFT_DIAL_COMPLICATION;
import static com.example.android.sunshine.ComplicationsHelper.TOP_DIAL_COMPLICATION;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {

    private static final String TAG = SunshineWatchFace.class.getSimpleName();

    /*
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final int BACKGROUND_WIDTH = 600;
        private static final int BACKGROUND_HEIGHT = 600;

        private static final float HOUR_STROKE_WIDTH = 8f;
        private static final float MINUTE_STROKE_WIDTH = HOUR_STROKE_WIDTH;
        private static final float HAND_DECORATION_STROKE_WIDTH = 4f;
        private static final float SECOND_STROKE_WIDTH = 2f;
        private static final float TICK_PRIMARY_STROKE_WIDTH = 5f;
        private static final float TICK_SECONDARY_STROKE_WIDTH = 2f;

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 10f;
        private static final float SECOND_HAND_CIRCLE_RADIUS = 7f;

        private static final int PRIMARY_SHADOW_RADIUS = 6;
        private static final int SECONDARY_SHADOW_RADIUS = 3;

        private static final float HOUR_LABEL_FONT_SIZE = 54f;

        private static final int TICK_PADDING = 8;
        private static final int TICK_LENGTH = 28;

        private final Rect mPeekCardBounds = new Rect();
        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;
        private float mCenterX;
        private float mCenterY;
        private float mSecondHandLength;
        private float sMinuteHandLength;
        private float sHourHandLength;
        /* Colors for all hands (hour, minute, seconds, ticks) based on photo loaded. */
        private int mWatchHandColor;
        private int mWatchHandDecorationColor;
        private int mWatchHandHighlightColor;
        private int mWatchDarkShadowColor;
        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mHandDecorationPaint;
        private Paint mSecondPaint;
        private Paint mSecondCircleBottomPaint;
        private Paint mSecondCircleTopPaint;
        private Paint mCircleBottomPaint;
        private Paint mCircleTopPaint;
        private Paint mTickPrimaryPaint;
        private Paint mTickSecondaryPaint;
        private Paint mBackgroundPaint;
        private Bitmap mBackgroundBitmap;
        private Bitmap mGrayBackgroundBitmap;
        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        private ComplicationsHelper mComplicationsHelper;
        private boolean mIsSquare;
        private int mSurfaceWidth;
        private int mSurfaceHeight;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setStatusBarGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL)
                    .setHotwordIndicatorGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL)
                    .setShowUnreadCountIndicator(true)
                    .setViewProtectionMode(PROTECT_STATUS_BAR | PROTECT_HOTWORD_INDICATOR)
                    .setAcceptsTapEvents(true)
                    .setHideHotwordIndicator(true)
                    .setHideStatusBar(true)
                    .build());

            /* Set defaults for colors */
            mWatchHandColor = Color.WHITE;
            mWatchHandDecorationColor = getColor(R.color.primary);
            mWatchHandHighlightColor = getColor(R.color.accent);
            mWatchDarkShadowColor = getColor(R.color.shadow_dark);

            mHourPaint = new Paint();
            mHourPaint.setColor(mWatchHandColor);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.SQUARE);
            mHourPaint.setShadowLayer(PRIMARY_SHADOW_RADIUS, 0, 0, mWatchDarkShadowColor);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mWatchHandColor);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.SQUARE);
            mMinutePaint.setShadowLayer(PRIMARY_SHADOW_RADIUS, 0, 0, mWatchDarkShadowColor);

            mHandDecorationPaint = new Paint();
            mHandDecorationPaint.setColor(mWatchHandDecorationColor);
            mHandDecorationPaint.setStrokeWidth(HAND_DECORATION_STROKE_WIDTH);
            mHandDecorationPaint.setAntiAlias(true);
            mHandDecorationPaint.setStrokeCap(Paint.Cap.SQUARE);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mWatchHandHighlightColor);
            mSecondPaint.setStrokeWidth(SECOND_STROKE_WIDTH);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);
            mSecondPaint.setShadowLayer(SECONDARY_SHADOW_RADIUS, 0, 0, mWatchDarkShadowColor);

            mSecondCircleBottomPaint = new Paint(mSecondPaint);
            mSecondCircleBottomPaint.setShadowLayer(SECONDARY_SHADOW_RADIUS, 0, 0, mWatchDarkShadowColor);

            mSecondCircleTopPaint = new Paint(mSecondCircleBottomPaint);
            mSecondCircleTopPaint.clearShadowLayer();

            mCircleBottomPaint = new Paint();
            mCircleBottomPaint.setColor(mWatchHandColor);
            mCircleBottomPaint.setStrokeWidth(SECOND_STROKE_WIDTH);
            mCircleBottomPaint.setAntiAlias(true);
            mCircleBottomPaint.setStyle(Paint.Style.FILL);
            mCircleBottomPaint.setShadowLayer(PRIMARY_SHADOW_RADIUS, 0, 0, mWatchDarkShadowColor);

            mCircleTopPaint = new Paint(mCircleBottomPaint);
            mCircleTopPaint.clearShadowLayer();

            mTickPrimaryPaint = new Paint();
            mTickPrimaryPaint.setColor(Color.WHITE);
            mTickPrimaryPaint.setStrokeWidth(TICK_PRIMARY_STROKE_WIDTH);
            mTickPrimaryPaint.setTextSize(HOUR_LABEL_FONT_SIZE);
            mTickPrimaryPaint.setTypeface(Typeface.createFromAsset(getAssets(),
                    "fonts/Kanit-Medium.ttf"));
            mTickPrimaryPaint.setAntiAlias(true);
            mTickPrimaryPaint.setStyle(Paint.Style.FILL);

            mTickSecondaryPaint = new Paint(mTickPrimaryPaint);
            mTickSecondaryPaint.setColor(getColor(R.color.primary_light));
            mTickSecondaryPaint.setStrokeWidth(TICK_SECONDARY_STROKE_WIDTH);
            mTickSecondaryPaint.setAntiAlias(true);

            // Initialise complications helper class
            mComplicationsHelper = new ComplicationsHelper(getApplicationContext());
            // Tells Android Wear complications are supported and passes their unique IDs
            setActiveComplications(COMPLICATION_IDS);

            mCalendar = Calendar.getInstance();
        }

        /**
         * Prepares bitmap to be used as watch face background.
         **/
        private Bitmap createBackgroundBitmap(boolean isAmbient) {
            // Define colours depending on ambient mode
            int backgroundColor = isAmbient ? Color.BLACK : getColor(R.color.primary_dark);
            int secondaryTickColor = isAmbient ? Color.WHITE : getColor(R.color.primary_light);
            mTickSecondaryPaint.setColor(secondaryTickColor);
            // Prepare background bitmap
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(backgroundColor);
            mBackgroundPaint.setAntiAlias(false);
            mBackgroundPaint.setFilterBitmap(true);
            Bitmap bitmap = Bitmap.createBitmap(BACKGROUND_WIDTH, BACKGROUND_HEIGHT,
                    Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(backgroundColor);

            // Draw logo on the background bitmap
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_logo);
            logo = Bitmap.createScaledBitmap(logo, (int) (logo.getWidth() * 0.7f),
                    (int) (logo.getHeight() * 0.7f), true);
            Canvas canvas = new Canvas(bitmap);
            float x = (bitmap.getWidth() * 0.7f) - (logo.getWidth() * 0.4f);
            float y = bitmap.getHeight() * 0.5f - (logo.getHeight() * 0.6f);

            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
             */
            if (mIsSquare) {
                Bitmap ticksBitmap = Bitmap.createBitmap(
                        BACKGROUND_WIDTH - TICK_PADDING * 2,
                        BACKGROUND_HEIGHT - TICK_PADDING * 2,
                        Bitmap.Config.ARGB_8888);
                Canvas ticksCanvas = new Canvas(ticksBitmap);

                int bitmapCenterX = ticksBitmap.getWidth() / 2;
                int bitmapCenterY = ticksBitmap.getHeight() / 2;
                float innerTickRadius = bitmapCenterX - 100f;
                float outerTickRadius = bitmapCenterX + 100f;

                // Draw ticks on 2nd layer
                for (int tickIndex = 0; tickIndex < 60; tickIndex++) {
                    float tickRot = (float) (tickIndex * Math.PI * 2 / 60);
                    float innerX = (float)  Math.sin(tickRot) * innerTickRadius;
                    float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                    float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                    float outerY = (float) -Math.cos(tickRot) * outerTickRadius;

                    if (tickIndex % 5 == 0) {
                        ticksCanvas.drawLine(bitmapCenterX + innerX, bitmapCenterY + innerY,
                                bitmapCenterX + outerX, bitmapCenterY + outerY, mTickPrimaryPaint);
                    } else {
                        ticksCanvas.drawLine(bitmapCenterX + innerX, bitmapCenterY + innerY,
                                bitmapCenterX + outerX, bitmapCenterY + outerY, mTickSecondaryPaint);
                    }
                }

                // Draw mask that will trim ticks
                RectF rectF = new RectF(new Rect(
                        TICK_LENGTH,
                        TICK_LENGTH,
                        ticksBitmap.getWidth() - TICK_LENGTH ,
                        ticksBitmap.getHeight() - TICK_LENGTH));

                ticksCanvas.drawRoundRect(rectF, 120f, 120f, mBackgroundPaint);

                // Draw hour labels on top of the mask
                int width = ticksBitmap.getWidth();
                int height = ticksBitmap.getHeight();

                // Coordinates of hour labels starting at "1"
                float[][] labelCoordinates = {
                        {0.75f * width, 0},
                        {width, 0.25f * height},
                        {width, 0.5f * height},
                        {width, 0.75f * height},
                        {0.75f * width, height},
                        {0.5f * width, height},
                        {0.25f * width, height},
                        {0, 0.75f * height},
                        {0, 0.5f * height},
                        {0, 0.25f * height},
                        {0.25f * width, 0},
                        {0.5f * width, 0}
                };

                for (int index = 0; index < labelCoordinates.length; index++) {
                    String label = Integer.toString(index + 1);

                    // Get label measurements
                    Rect textBounds = new Rect();
                    mTickPrimaryPaint.getTextBounds(label, 0, 1, textBounds);
                    float textWidth = mTickPrimaryPaint.measureText(label);

                    // Calculate label offsets
                    float offsetX;
                    float offsetY;

                    if (index == 0 || index > 9) {
                        offsetX = -textWidth / 2f;
                        offsetY = TICK_PADDING * 2 + TICK_LENGTH + textBounds.height();
                    } else if (index > 0 && index < 4) {
                        offsetX = -TICK_PADDING * 2f - TICK_LENGTH - textWidth;
                        offsetY = textBounds.height() / 2f;
                    } else if (index > 3 && index < 7) {
                        offsetX = -textWidth / 2f;
                        offsetY = -TICK_PADDING * 2f - TICK_LENGTH;
                    } else {
                        offsetX = TICK_PADDING * 2f + TICK_LENGTH;
                        offsetY = textBounds.height() / 2f;
                    }

                    // Draw hour labels
                    ticksCanvas.drawText(label,
                            labelCoordinates[index][0] + offsetX,
                            labelCoordinates[index][1] + offsetY,
                            mTickPrimaryPaint);
                }

                // Draw bitmap with ticks on watch face canvas
                canvas.drawBitmap(ticksBitmap, TICK_PADDING, TICK_PADDING, mBackgroundPaint);
            } else {
                int bitmapCenterX = BACKGROUND_WIDTH / 2;
                int bitmapCenterY = BACKGROUND_HEIGHT / 2;
                float innerTickRadius = bitmapCenterX - 34f;
                float outerTickRadius = bitmapCenterX - 7f;
                float innerHourRadius = bitmapCenterX - 60f;

                int[] hours = {12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
                int hourIndex = 0;

                for (int tickIndex = 0; tickIndex < 60; tickIndex++) {
                    float tickRot = (float) (tickIndex * Math.PI * 2 / 60);
                    float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                    float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                    float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                    float outerY = (float) -Math.cos(tickRot) * outerTickRadius;

                    if (tickIndex % 5 == 0) {
                        canvas.drawLine(bitmapCenterX + innerX, bitmapCenterY + innerY,
                                bitmapCenterX + outerX, bitmapCenterY + outerY, mTickPrimaryPaint);

                        float innerHourX = (float) (Math.sin(tickRot) * innerHourRadius) - 15f;
                        float innerHourY = (float) (-Math.cos(tickRot) * innerHourRadius) + 18f;
                        if (hourIndex == 0) {
                            innerHourX -= 5f;
                        }

                        canvas.drawText(Integer.toString(hours[hourIndex++]),
                                bitmapCenterX + innerHourX, bitmapCenterY + innerHourY, mTickPrimaryPaint);
                    } else {
                        canvas.drawLine(bitmapCenterX + innerX, bitmapCenterY + innerY,
                                bitmapCenterX + outerX, bitmapCenterY + outerY, mTickSecondaryPaint);
                    }
                }
            }
            canvas.drawBitmap(logo, x, y, null);

            return bitmap;
        }

        @Override
        public void onComplicationDataUpdate(int watchFaceComplicationId, ComplicationData data) {
            // Adds/updates active complication data in the array.
            mComplicationsHelper.updateComplicationsArray(watchFaceComplicationId, data);
            // Invalidate the screen so onDraw() is called
            invalidate();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            mComplicationsHelper.setAmbientMode(inAmbientMode);
            updateWatchHandStyle();

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            // Determine wearable's shape
            mIsSquare = !insets.isRound();

            /* Create watch face background bitmap */
            mBackgroundBitmap = createBackgroundBitmap(false);

            /* Scale loaded background image (more efficient) if surface dimensions change. */
            float scale = ((float) mSurfaceWidth) / (float) mBackgroundBitmap.getWidth();
            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (int) (mBackgroundBitmap.getWidth() * scale),
                    (int) (mBackgroundBitmap.getHeight() * scale), true);

            // Recalculate surface changes
            mComplicationsHelper.recalculateComplicationsPositions(mSurfaceWidth, mSurfaceHeight);

            /*
             * Create a gray version of the image only if it will look nice on the device in
             * ambient mode. That means we don't want devices that support burn-in
             * protection (slight movements in pixels, not great for images going all the way to
             * edges) and low ambient mode (degrades image quality).
             *
             * Also, if your watch face will know about all images ahead of time (users aren't
             * selecting their own photos for the watch face), it will be more
             * efficient to create a black/white version (png, etc.) and load that when you need it.
             */
            if (!mBurnInProtection) {
                mGrayBackgroundBitmap = createBackgroundBitmap(true);
                mGrayBackgroundBitmap = Bitmap.createScaledBitmap(mGrayBackgroundBitmap,
                        (int) (mGrayBackgroundBitmap.getWidth() * scale),
                        (int) (mGrayBackgroundBitmap.getHeight() * scale), true);
            }
        }

        private void updateWatchHandStyle() {
            if (mAmbient) {
                mHourPaint.setColor(Color.WHITE);
                mMinutePaint.setColor(Color.WHITE);
                mSecondPaint.setColor(Color.WHITE);
                mCircleBottomPaint.setColor(Color.WHITE);
                mHandDecorationPaint.setColor(Color.BLACK);

                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
                mHandDecorationPaint.setAntiAlias(false);
                mCircleBottomPaint.setAntiAlias(false);
                mCircleTopPaint.setAntiAlias(false);
                mCircleTopPaint.setColor(Color.BLACK);

                mBackgroundPaint.setFilterBitmap(false);

                mHourPaint.clearShadowLayer();
                mMinutePaint.clearShadowLayer();
                mCircleBottomPaint.clearShadowLayer();

            } else {
                mHourPaint.setColor(mWatchHandColor);
                mMinutePaint.setColor(mWatchHandColor);
                mSecondPaint.setColor(mWatchHandHighlightColor);
                mCircleBottomPaint.setColor(mWatchHandColor);
                mHandDecorationPaint.setColor(mWatchHandDecorationColor);

                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
                mHandDecorationPaint.setAntiAlias(true);
                mCircleBottomPaint.setAntiAlias(true);
                mCircleTopPaint.setAntiAlias(true);
                mCircleTopPaint.setColor(Color.WHITE);

                mBackgroundPaint.setFilterBitmap(true);

                mHourPaint.setShadowLayer(PRIMARY_SHADOW_RADIUS, 0, 0, mWatchDarkShadowColor);
                mMinutePaint.setShadowLayer(PRIMARY_SHADOW_RADIUS, 0, 0, mWatchDarkShadowColor);
                mCircleBottomPaint.setShadowLayer(PRIMARY_SHADOW_RADIUS, 0, 0, mWatchDarkShadowColor);
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            // Get actual width and height of wearable's display
            mSurfaceWidth = width;
            mSurfaceHeight = height;

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            /*
             * Calculate lengths of different hands based on watch screen size.
             */
            mSecondHandLength = (float) (mCenterX * 0.8);
            sMinuteHandLength = (float) (mCenterX * 0.8);
            sHourHandLength = (float) (mCenterX * 0.5);

            // Set default complications
            setDefaultComplicationProvider(TOP_DIAL_COMPLICATION,
                    new ComponentName(getPackageName(),
                            "com.example.android.sunshine.complications.TemperatureProviderService"),
                    COMPLICATION_SUPPORTED_TYPES[0][0]);
            setDefaultComplicationProvider(LEFT_DIAL_COMPLICATION,
                    new ComponentName(getPackageName(),
                            "com.example.android.sunshine.complications.SummaryProviderService"),
                    COMPLICATION_SUPPORTED_TYPES[1][0]);
            setDefaultComplicationProvider(BOTTOM_DIAL_COMPLICATION,
                    new ComponentName(getPackageName(),
                            "com.example.android.sunshine.complications.HumidityProviderService"),
                    COMPLICATION_SUPPORTED_TYPES[2][0]);
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    int tappedComplicationId = mComplicationsHelper.getTappedComplicationId(x, y);
                    if (tappedComplicationId != -1) {
                        mComplicationsHelper.onComplicationTap(tappedComplicationId);
                    }
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            // Draw Background
            if (mAmbient && mBurnInProtection) {
                canvas.drawColor(Color.BLACK);
            } else if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);
            }

            // Draw Complications if device is not in ambient mode
            if (!mAmbient) {
                mComplicationsHelper.drawComplications(canvas, now);
            }

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save();

            canvas.drawCircle(
                    mCenterX,
                    mCenterY,
                    CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCircleBottomPaint);

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - sHourHandLength,
                    mHourPaint);
            // Draw hand decoration
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS - (sHourHandLength * 0.2f),
                    mCenterX,
                    mCenterY - sHourHandLength + (sHourHandLength * 0.04f),
                    mHandDecorationPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - sMinuteHandLength,
                    mMinutePaint);
            // Draw hand decoration
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS - (sMinuteHandLength * 0.2f),
                    mCenterX,
                    mCenterY - sMinuteHandLength + (sMinuteHandLength * 0.04f),
                    mHandDecorationPaint);

            // Draw top circle
            if (mAmbient) {
                canvas.drawCircle(
                        mCenterX,
                        mCenterY,
                        SECOND_HAND_CIRCLE_RADIUS,
                        mCircleTopPaint);
            } else {
                canvas.drawCircle(
                        mCenterX,
                        mCenterY,
                        CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCircleTopPaint);
            }

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawCircle(
                        mCenterX,
                        mCenterY,
                        SECOND_HAND_CIRCLE_RADIUS,
                        mSecondCircleBottomPaint);

                float topY = mSecondHandLength + (CENTER_GAP_AND_CIRCLE_RADIUS * 0.5f) - 1f;
                float bottomY = mSecondHandLength * 0.22f;

                Path path = new Path();
                path.moveTo(mCenterX - 1f, mCenterY - topY); // Top
                path.lineTo(mCenterX - 2f, mCenterY); // Middle left
                path.lineTo(mCenterX - 3f, mCenterY + bottomY); // Bottom left
                path.lineTo(mCenterX + 3f, mCenterY + bottomY); // Bottom right
                path.lineTo(mCenterX + 2f, mCenterY); // Middle right
                path.lineTo(mCenterX + 1f, mCenterY - topY); // Back to Top
                path.close();
                canvas.drawPath(path, mSecondPaint);

                canvas.drawCircle(
                        mCenterX,
                        mCenterY,
                        SECOND_HAND_CIRCLE_RADIUS,
                        mSecondCircleTopPaint);
            }

            /* Restore the canvas' original orientation. */
            canvas.restore();

            /* Draw rectangle behind peek card in ambient mode to improve readability. */
            if (mAmbient) {
                canvas.drawRect(mPeekCardBounds, mBackgroundPaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        @Override
        public void onPeekCardPositionUpdate(Rect rect) {
            super.onPeekCardPositionUpdate(rect);
            mPeekCardBounds.set(rect);
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
