package com.example.android.sunshine;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;
import android.util.SparseArray;


/**
 * Helper class that draws complications on the Sunshine
 */

public class ComplicationsHelper {

    private static String TAG = ComplicationsHelper.class.getSimpleName();

    /**
     * Constants defining available complications
     */
    public static final int TOP_DIAL_COMPLICATION = 0;
    public static final int LEFT_DIAL_COMPLICATION = 1;
    public static final int BOTTOM_DIAL_COMPLICATION = 2;

    public static final int[] COMPLICATION_IDS = {TOP_DIAL_COMPLICATION, LEFT_DIAL_COMPLICATION,
            BOTTOM_DIAL_COMPLICATION};

    // Left and right dial supported types.
    public static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {ComplicationData.TYPE_RANGED_VALUE, ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SMALL_IMAGE},
            {ComplicationData.TYPE_RANGED_VALUE, ComplicationData.TYPE_SHORT_TEXT}
    };

    /**
     * Constants used to draw complications
     */
    private static final float COMPLICATION_STROKE_WIDTH = 1f;
    private static final float COMPLICATIONS_PRIMARY_FONT_SIZE = 36f;
    private static final float COMPLICATIONS_SECONDARY_FONT_SIZE = 24f;

    // Variables for painting Complications
    private Paint mComplicationPaint;
    private Paint mComplicationBackgroundPaint;
    private Paint mComplicationSecondaryPaint;
    private Paint mComplicationStrokePaint;

    // X and Y coordinates used to place complications properly
    private int mTopComplicationX;
    private int mTopComplicationY;
    private int mLeftComplicationX;
    private int mLeftComplicationY;
    private int mBottomComplicationY;

    // Complication radius
    private float mComplicationRadius;

    /* Maps active complication ids to the data for that complication. Note: Data will only be
     * present if the user has chosen a provider via the settings activity for the watch face.
     */
    private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

    private Context mContext;

    private int mBackgroundWidth;
    private int mBackgroundHeight;

    ComplicationsHelper(Context context) {
        mContext = context;

        mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

        mComplicationPaint = new Paint();
        mComplicationPaint.setColor(Color.WHITE);
        mComplicationPaint.setTextSize(COMPLICATIONS_PRIMARY_FONT_SIZE);
        mComplicationPaint.setTypeface(Typeface.createFromAsset(context.getAssets(),
                "fonts/Kanit-Light.ttf"));
        mComplicationPaint.setAntiAlias(true);

        mComplicationBackgroundPaint = new Paint();
        mComplicationBackgroundPaint.setColor(context.getColor(R.color.primary_dark));
        mComplicationBackgroundPaint.setAntiAlias(true);

        mComplicationSecondaryPaint = new Paint();
        mComplicationSecondaryPaint.setColor(context.getColor(R.color.primary_light));
        mComplicationSecondaryPaint.setTextSize(COMPLICATIONS_SECONDARY_FONT_SIZE);
        mComplicationSecondaryPaint.setTypeface(Typeface.createFromAsset(context.getAssets(),
                "fonts/Kanit-Light.ttf"));
        mComplicationSecondaryPaint.setAntiAlias(true);

        mComplicationStrokePaint = new Paint();
        mComplicationStrokePaint.setColor(context.getColor(R.color.primary_light));
        mComplicationStrokePaint.setStrokeWidth(COMPLICATION_STROKE_WIDTH);
        mComplicationStrokePaint.setAntiAlias(true);
        mComplicationStrokePaint.setStyle(Paint.Style.STROKE);
    }


    /**
     * Draws complications on the watch face.
     *
     * @param canvas            where complications are drawn
     * @param currentTimeMillis current time in milliseconds
     */
    public void drawComplications(Canvas canvas, long currentTimeMillis) {
        ComplicationData complicationData;

        for (int i = 0; i < COMPLICATION_IDS.length; i++) {

            complicationData = mActiveComplicationDataSparseArray.get(COMPLICATION_IDS[i]);

            if ((complicationData != null)
                    && (complicationData.isActive(currentTimeMillis))) {

                // Top & Bottom short text complications
                if (complicationData.getType() == ComplicationData.TYPE_SHORT_TEXT
                        || complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {
                    drawShortTextComplication(
                            canvas,
                            currentTimeMillis,
                            complicationData,
                            COMPLICATION_IDS[i]);
                }
                // Left small image complications
                if (complicationData.getType() == ComplicationData.TYPE_SMALL_IMAGE
                        || complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {
                    drawSmallImageComplication(
                            canvas,
                            complicationData);
                }

                // Bottom ranged value complication
                if (complicationData.getType() == ComplicationData.TYPE_RANGED_VALUE
                        || complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {
                    ComplicationText shortText = complicationData.getShortText();
                    ComplicationText shortTitle = complicationData.getShortTitle();

                    CharSequence shortTextMessage =
                            shortText.getText(mContext, currentTimeMillis);

                    int complicationY;

                    if (COMPLICATION_IDS[i] == TOP_DIAL_COMPLICATION) {
                        complicationY = mTopComplicationY;
                    } else if (COMPLICATION_IDS[i] == BOTTOM_DIAL_COMPLICATION) {
                        complicationY = mBottomComplicationY;
                    } else {
                        complicationY = mLeftComplicationY;
                    }
                    // Complication background
                    canvas.drawCircle(
                            mTopComplicationX,
                            complicationY + mComplicationRadius,
                            mComplicationRadius,
                            mComplicationBackgroundPaint);

                    // Complication stroke
                    canvas.drawCircle(
                            mTopComplicationX,
                            complicationY + mComplicationRadius,
                            mComplicationRadius,
                            mComplicationStrokePaint);

                    // TODO: Markings
                        /*
                         * Draw ticks. Usually you will want to bake this directly into the photo, but in
                         * cases where you want to allow users to select their own photos, this dynamically
                         * creates them on top of the photo.
                         */
                    int bitmapCenterX = mTopComplicationX / 2;
                    int bitmapCenterY = complicationY / 2;
                    float innerTickRadius = bitmapCenterX - 34f;
                    float outerTickRadius = bitmapCenterX - 7f;
                    float innerHourRadius = bitmapCenterX - 60f;

                    int hours[] = {12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
                    int hourIndex = 0;

                    for (int tickIndex = 0; tickIndex < 60; tickIndex++) {
                        float tickRot = (float) (tickIndex * Math.PI * 2 / 60);
                        float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                        float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                        float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                        float outerY = (float) -Math.cos(tickRot) * outerTickRadius;

                        if (tickIndex % 5 == 0) {
                            canvas.drawLine(bitmapCenterX + innerX, bitmapCenterY + innerY,
                                    bitmapCenterX + outerX, bitmapCenterY + outerY, mComplicationPaint);

                            float innerHourX = (float) (Math.sin(tickRot) * innerHourRadius) - 15f;
                            float innerHourY = (float) (-Math.cos(tickRot) * innerHourRadius) + 18f;
                            if (hourIndex == 0) {
                                innerHourX -= 5f;
                            }

                            canvas.drawText(Integer.toString(hours[hourIndex++]),
                                    bitmapCenterX + innerHourX, bitmapCenterY + innerHourY, mComplicationPaint);
                        } else {
                            canvas.drawLine(bitmapCenterX + innerX, bitmapCenterY + innerY,
                                    bitmapCenterX + outerX, bitmapCenterY + outerY, mComplicationPaint);
                        }
                    }


                    // TODO: Markings labels
                    // TODO: Hand
                    // TODO: Title label

                    float textWidth =
                            mComplicationPaint.measureText(
                                    shortTextMessage,
                                    0,
                                    shortTextMessage.length());

                    float offsetX = textWidth / 2;
                    float offsetY = mComplicationRadius;
                    if (shortTitle == null) {
                        Rect textBounds = new Rect();
                        mComplicationPaint.getTextBounds(shortTextMessage.toString(),
                                0, 1, textBounds);

                        offsetY += textBounds.height() / 2;
                    }

                    // Complication short text
                    canvas.drawText(
                            shortTextMessage,
                            0,
                            shortTextMessage.length(),
                            mTopComplicationX - offsetX,
                            complicationY + offsetY,
                            mComplicationPaint);

                    // Complication short title
                    if (shortTitle != null) {
                        CharSequence shortTitleMessage =
                                shortTitle.getText(mContext, currentTimeMillis);

                        offsetX = mComplicationSecondaryPaint.measureText(
                                shortTitleMessage,
                                0,
                                shortTitleMessage.length()) / 2;
                        offsetY = 1.5f * mComplicationRadius;

                        canvas.drawText(
                                shortTitleMessage,
                                0,
                                shortTitleMessage.length(),
                                mTopComplicationX - offsetX,
                                complicationY + offsetY,
                                mComplicationSecondaryPaint);
                    }
                }
            }
        }
    }

    /**
     * Draws top & bottom short text complications.
     *
     * @param canvas on which to draw
     * @param now    current time in milliseconds
     * @param data   to be drawn
     * @param id     of the complication
     */
    private void drawShortTextComplication(Canvas canvas, long now, ComplicationData data,
                                           int id) {
        ComplicationText shortText = data.getShortText();
        ComplicationText shortTitle = data.getShortTitle();

        CharSequence shortTextMessage =
                shortText.getText(mContext, now);

        int complicationY;

        if (id == TOP_DIAL_COMPLICATION) {
            complicationY = mTopComplicationY;
        } else if (id == BOTTOM_DIAL_COMPLICATION) {
            complicationY = mBottomComplicationY;
        } else {
            complicationY = mLeftComplicationY;
        }
        // Complication background
        canvas.drawCircle(
                mTopComplicationX,
                complicationY + mComplicationRadius,
                mComplicationRadius,
                mComplicationBackgroundPaint);

        // Complication stroke
        canvas.drawCircle(
                mTopComplicationX,
                complicationY + mComplicationRadius,
                mComplicationRadius,
                mComplicationStrokePaint);

        float textWidth =
                mComplicationPaint.measureText(
                        shortTextMessage,
                        0,
                        shortTextMessage.length());

        float offsetX = textWidth / 2;
        float offsetY = mComplicationRadius;
        if (shortTitle == null) {
            Rect textBounds = new Rect();
            mComplicationPaint.getTextBounds(shortTextMessage.toString(),
                    0, 1, textBounds);

            offsetY += textBounds.height() / 2;
        }

        // Complication short text
        canvas.drawText(
                shortTextMessage,
                0,
                shortTextMessage.length(),
                mTopComplicationX - offsetX,
                complicationY + offsetY,
                mComplicationPaint);

        // Complication short title
        if (shortTitle != null) {
            CharSequence shortTitleMessage =
                    shortTitle.getText(mContext, now);

            offsetX = mComplicationSecondaryPaint.measureText(
                    shortTitleMessage,
                    0,
                    shortTitleMessage.length()) / 2;
            offsetY = 1.5f * mComplicationRadius;

            canvas.drawText(
                    shortTitleMessage,
                    0,
                    shortTitleMessage.length(),
                    mTopComplicationX - offsetX,
                    complicationY + offsetY,
                    mComplicationSecondaryPaint);
        }
    }

    /**
     * Draws left small image complications.
     *
     * @param canvas on which to draw
     * @param data   to be drawn
     */
    private void drawSmallImageComplication(Canvas canvas, ComplicationData data) {
        BitmapDrawable imageDrawable = (BitmapDrawable) data.getSmallImage()
                .loadDrawable(mContext);
        Bitmap imageBitmap = imageDrawable.getBitmap();

        float widthScale = (mBackgroundWidth * 0.5f * 0.35f) / imageBitmap.getWidth();
        float heightScale = (mBackgroundHeight * 0.5f * 0.35f) / imageBitmap.getHeight();
        int scaledWidth = (int) (widthScale * imageBitmap.getWidth());
        int scaledHeight = (int) (heightScale * imageBitmap.getHeight());

        // Create the RoundedBitmapDrawable.
        RoundedBitmapDrawable roundDrawable = RoundedBitmapDrawableFactory.create(
                mContext.getResources(),
                Bitmap.createScaledBitmap(imageBitmap, scaledWidth, scaledHeight, false));
        roundDrawable.setCircular(true);

        int startX = mLeftComplicationX - scaledWidth / 2;
        int startY = mLeftComplicationY - scaledHeight / 2;
        int finishX = mLeftComplicationX + scaledWidth / 2;
        int finishY = mLeftComplicationY + scaledHeight / 2;
        roundDrawable.setBounds(startX, startY, finishX, finishY);
        roundDrawable.draw(canvas);
    }

    // Fires PendingIntent associated with complication (if it has one).
    public void onComplicationTap(int complicationId) {
        ComplicationData complicationData =
                mActiveComplicationDataSparseArray.get(complicationId);

        if (complicationData != null) {

            if (complicationData.getTapAction() != null) {
                try {
                    complicationData.getTapAction().send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "onComplicationTap() tap action error: " + e);
                }

            } else if (complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {

                // Watch face does not have permission to receive complication data, so launch
                // permission request.
                ComponentName componentName = new ComponentName(
                        mContext,
                        SunshineWatchFace.class);

                Intent permissionRequestIntent =
                        ComplicationHelperActivity.createPermissionRequestHelperIntent(
                                mContext, componentName);

                mContext.startActivity(permissionRequestIntent);
            }

        } else {
            Log.d(TAG, "No PendingIntent for complication " + complicationId + ".");
        }
    }

    /*
    * Determines if tap inside a complication area or returns -1.
    */
    public int getTappedComplicationId(int touchX, int touchY) {
        ComplicationData complicationData;
        long currentTimeMillis = System.currentTimeMillis();

        for (int i = 0; i < COMPLICATION_IDS.length; i++) {
            complicationData = mActiveComplicationDataSparseArray.get(COMPLICATION_IDS[i]);

            if ((complicationData != null)
                    && (complicationData.isActive(currentTimeMillis))
                    && (complicationData.getType() != ComplicationData.TYPE_NOT_CONFIGURED)
                    && (complicationData.getType() != ComplicationData.TYPE_EMPTY)) {

                int complicationX = 0;
                int complicationY = 0;
                int radius = 0;

                switch (COMPLICATION_IDS[i]) {
                    case TOP_DIAL_COMPLICATION:
                        radius = (int) mComplicationRadius;
                        complicationX = mTopComplicationX;
                        complicationY = mTopComplicationY + radius;
                        break;

                    case LEFT_DIAL_COMPLICATION:
                        radius = (int) (mBackgroundWidth * 0.5f * 0.35f) / 2;
                        complicationX = mLeftComplicationX;
                        complicationY = mLeftComplicationY;
                        break;

                    case BOTTOM_DIAL_COMPLICATION:
                        radius = (int) mComplicationRadius;
                        complicationX = mTopComplicationX;
                        complicationY = mBottomComplicationY + radius;
                        break;
                }

                // Distance between two points formula
                float touchRadius = (float) Math.sqrt(Math.pow(complicationX - touchX, 2)
                        + Math.pow(complicationY - touchY, 2));

                if (touchRadius < radius) {
                    Log.d(TAG, "getTappedComplicationId: " + COMPLICATION_IDS[i]);
                    return COMPLICATION_IDS[i];
                } else {
                    Log.e(TAG, "Not a recognized complication id.");
                }
            }
        }
        return -1;
    }

    /**
     * Adds/updates active complication data in the array.
     * @param watchFaceComplicationId id of the complication to update
     * @param data complication data
     */
    public void updateComplicationsArray(int watchFaceComplicationId, ComplicationData data) {
        mActiveComplicationDataSparseArray.put(watchFaceComplicationId, data);
    }

    /**
     * Turns anti aliasing on and off.
     * @param inAmbientMode true if device is in ambient mode
     */
    public void setAmbientMode(boolean inAmbientMode){
        mComplicationPaint.setAntiAlias(!inAmbientMode);
    }

    public void recalculateComplicationsPositions(int backgroundWidth, int backgroundHeight) {
        mBackgroundWidth = backgroundWidth;
        mBackgroundHeight = backgroundHeight;
        mComplicationRadius = mBackgroundWidth / 7f;
        mTopComplicationX = mBackgroundWidth / 2;
        mTopComplicationY = (mBackgroundHeight / 2) - (int) (2.3 * mComplicationRadius);
        mLeftComplicationX = (mBackgroundWidth / 4) + (mBackgroundWidth / 32);
        mLeftComplicationY = mBackgroundHeight / 2;
        mBottomComplicationY = (mBackgroundHeight / 2) + (int) (0.3 * mComplicationRadius);
    }
}
