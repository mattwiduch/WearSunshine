package com.example.android.sunshine;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
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

class ComplicationsHelper {
    private static final String TAG = ComplicationsHelper.class.getSimpleName();

    /**
     * Constants defining available complications
     */
    static final int TOP_DIAL_COMPLICATION = 0;
    static final int LEFT_DIAL_COMPLICATION = 1;
    static final int BOTTOM_DIAL_COMPLICATION = 2;

    static final int[] COMPLICATION_IDS = {TOP_DIAL_COMPLICATION, LEFT_DIAL_COMPLICATION,
            BOTTOM_DIAL_COMPLICATION};

    // Supported types (top, left, bottom)
    static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {ComplicationData.TYPE_SHORT_TEXT},
            {ComplicationData.TYPE_SMALL_IMAGE},
            {ComplicationData.TYPE_RANGED_VALUE, ComplicationData.TYPE_SHORT_TEXT}
    };

    /**
     * Constants used to draw complications
     */
    private static final float COMPLICATION_STROKE_WIDTH = 4f;
    private static final float COMPLICATION_MASK_STROKE_WIDTH = 100f;
    private static final float COMPLICATION_PRIMARY_FONT_SIZE = 36f;
    private static final float COMPLICATION_SECONDARY_FONT_SIZE = 24f;
    private static final float COMPLICATION_TICK_FONT_SIZE = 9f;
    private static final int COMPLICATION_TEXT_MAXIMUM_LENGTH = 7;
    private static final float COMPLICATION_PRIMARY_SHADOW_RADIUS = 4f;
    private static final float COMPLICATION_SECONDARY_SHADOW_RADIUS = 2f;
    private static final int CANVAS_ROTATION_DEGREES = 210;

    // Variables for painting Complications
    private Paint mComplicationPaint;
    private Paint mComplicationBackgroundPaint;
    private Paint mComplicationSecondaryPaint;
    private Paint mComplicationStrokePaint;
    private Paint mComplicationHandPaint;
    private Paint mComplicationTickPaint;
    private Paint mComplicationMaskPaint;

    // X and Y coordinates used to place complications properly
    private int mTopComplicationX;
    private int mTopComplicationY;
    private int mLeftComplicationX;
    private int mLeftComplicationY;
    private int mBottomComplicationY;

    // Complication radius
    private float mComplicationRadius;

    // Complications' background bitmaps
    private Bitmap mRangeComplicationBackground;
    private Bitmap mTextComplicationBackground;

    // Maps active complication ids to the data for that complication. Note: Data will only be
    // present if the user has chosen a provider via the settings activity for the watch face.
    private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

    private Context mContext;

    private int mBackgroundWidth;
    private int mBackgroundHeight;

    ComplicationsHelper(Context context) {
        mContext = context;

        mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

        mComplicationPaint = new Paint();
        mComplicationPaint.setColor(Color.WHITE);
        mComplicationPaint.setTextSize(COMPLICATION_PRIMARY_FONT_SIZE);
        mComplicationPaint.setTypeface(Typeface.createFromAsset(context.getAssets(),
                "fonts/Kanit-Light.ttf"));
        mComplicationPaint.setAntiAlias(true);

        mComplicationBackgroundPaint = new Paint();
        mComplicationBackgroundPaint.setColor(context.getColor(R.color.primary));
        mComplicationBackgroundPaint.setAntiAlias(true);

        mComplicationSecondaryPaint = new Paint();
        mComplicationSecondaryPaint.setColor(context.getColor(R.color.primary_light));
        mComplicationSecondaryPaint.setTextSize(COMPLICATION_SECONDARY_FONT_SIZE);
        mComplicationSecondaryPaint.setTypeface(Typeface.createFromAsset(context.getAssets(),
                "fonts/Kanit-Light.ttf"));
        mComplicationSecondaryPaint.setAntiAlias(true);

        mComplicationStrokePaint = new Paint();
        mComplicationStrokePaint.setColor(context.getColor(R.color.primary_light));
        mComplicationStrokePaint.setStrokeWidth(COMPLICATION_STROKE_WIDTH);
        mComplicationStrokePaint.setAntiAlias(true);
        mComplicationStrokePaint.setStyle(Paint.Style.STROKE);
        mComplicationStrokePaint.setShadowLayer(COMPLICATION_PRIMARY_SHADOW_RADIUS, 0, 0,
                mContext.getColor(R.color.shadow_light));

        mComplicationHandPaint = new Paint();
        mComplicationHandPaint.setColor(mContext.getColor(R.color.accent));
        mComplicationHandPaint.setAntiAlias(true);
        mComplicationHandPaint.setShadowLayer(COMPLICATION_SECONDARY_SHADOW_RADIUS, 0, 0,
                mContext.getColor(R.color.shadow_light));

        mComplicationTickPaint = new Paint();
        mComplicationTickPaint.setColor(Color.WHITE);
        mComplicationTickPaint.setTextSize(COMPLICATION_TICK_FONT_SIZE);
        mComplicationTickPaint.setTypeface(Typeface.createFromAsset(context.getAssets(),
                "fonts/Kanit-Medium.ttf"));
        mComplicationTickPaint.setAntiAlias(true);

        mComplicationMaskPaint = new Paint();
        mComplicationMaskPaint.setStyle(Paint.Style.STROKE);
        mComplicationMaskPaint.setStrokeWidth(COMPLICATION_MASK_STROKE_WIDTH);
        mComplicationMaskPaint.setColor(mContext.getColor(R.color.primary_dark));
        mComplicationMaskPaint.setAntiAlias(true);
    }

    /**
     * Draws complications on the watch face.
     *
     * @param canvas            where complications are drawn
     * @param currentTimeMillis current time in milliseconds
     */
    void drawComplications(Canvas canvas, long currentTimeMillis) {
        ComplicationData complicationData;

        for (int COMPLICATION_ID : COMPLICATION_IDS) {
            complicationData = mActiveComplicationDataSparseArray.get(COMPLICATION_ID);

            if ((complicationData != null)
                    && (complicationData.isActive(currentTimeMillis))) {
                // Top & Bottom short text complications
                if (complicationData.getType() == ComplicationData.TYPE_SHORT_TEXT
                        || complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {
                    drawShortTextComplication(
                            canvas,
                            currentTimeMillis,
                            complicationData,
                            COMPLICATION_ID);
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
                    drawRangeComplication(
                            canvas,
                            currentTimeMillis,
                            complicationData);
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

        // Draw complication background
        canvas.drawBitmap(
                mTextComplicationBackground,
                mTopComplicationX - mTextComplicationBackground.getWidth() * 0.5f,
                complicationY,
                null);

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

    /**
     * Draws ranged value complication.
     *
     * @param canvas           on which to draw
     * @param complicationData to be drawn
     * @param now              current time in milliseconds
     */
    private void drawRangeComplication(Canvas canvas, long now, ComplicationData complicationData) {
        // Define complication background and it's size
        Bitmap bitmap = Bitmap.createBitmap(mRangeComplicationBackground);
        Canvas complicationCanvas = new Canvas(bitmap);
        float width = (float) bitmap.getWidth();
        float height = (float) bitmap.getHeight();
        float centerX = width * 0.5f;
        float centerY = height * 0.5f;
        float radius = (width * 0.5f) - 1f;

        ComplicationText shortTitle = complicationData.getShortTitle();
        Icon icon = complicationData.getIcon();

        /* Display complication icon or title if available */
        if (icon != null) {
            // Prepare icon drawable
            int iconWidth = (int) (width * 0.2f);
            int iconHeight = (int) (height * 0.2f);
            Drawable iconDrawable = icon.loadDrawable(mContext);
            iconDrawable.setBounds(0, 0, iconWidth, iconHeight);

            // Calculate offsets so the icon is drawn centered at desired location
            float offsetX = width * 0.5f - iconWidth * 0.5f;
            float offsetY = height * 0.7f - iconWidth * 0.5f;

            // Translate the canvas to draw icon at desired location
            complicationCanvas.translate(offsetX, offsetY);
            iconDrawable.draw(complicationCanvas);
            // Move canvas back to its original position
            complicationCanvas.translate(-offsetX, -offsetY);
        } else if (shortTitle != null) {
            // Get short title text &
            CharSequence shortTitleMessage = shortTitle.getText(mContext, now);
            String text = shortTitleMessage.toString();
            text = text.toUpperCase();
            int endIndex = text.length() < COMPLICATION_TEXT_MAXIMUM_LENGTH ? text.length() : 7;
            text = text.substring(0, endIndex);

            // Get text bounds
            Rect textBounds = new Rect();
            mComplicationTickPaint.getTextBounds(text, 0, text.length(), textBounds);

            // Calculate offsets so text is drawn centered at desired location
            float offsetX = textBounds.width() / 2f;
            float offsetY = height * 0.25f - textBounds.height() / 2f;

            // Draw text
            complicationCanvas.drawText(
                    text,
                    0,
                    text.length(),
                    centerX - offsetX,
                    centerY + offsetY,
                    mComplicationTickPaint);
        }

        /* Draw hand */
        // Rotate the canvas so the range starts at 210 rather than 0 degrees
        complicationCanvas.rotate(CANVAS_ROTATION_DEGREES, centerX, centerY);
        // Rotate complication's hand (degrees in scale times value percent)
        float degrees = 300f * (complicationData.getValue() / complicationData.getMaxValue());
        complicationCanvas.rotate(degrees, centerX, centerY);

        // Draw hand's bottom circle
        complicationCanvas.drawCircle(
                centerX,
                centerY,
                3f,
                mComplicationHandPaint);

        // Calculate hand's length
        float topY = radius + (4f * 0.5f) - 10f;
        float bottomY = radius * 0.22f;

        // Draw hand's pointer
        Path path = new Path();
        path.moveTo(centerX - 0.5f, centerY - topY); // Top
        path.lineTo(centerX - 1f, centerY); // Middle left
        path.lineTo(centerX - 1.5f, centerY + bottomY); // Bottom left
        path.lineTo(centerX + 1.5f, centerY + bottomY); // Bottom right
        path.lineTo(centerX + 1f, centerY); // Middle right
        path.lineTo(centerX + 0.5f, centerY - topY); // Back to Top
        path.close();
        complicationCanvas.drawPath(path, mComplicationHandPaint);

        // Draw hand's top circle without shadow
        mComplicationHandPaint.clearShadowLayer();
        complicationCanvas.drawCircle(
                centerX,
                centerY,
                3f,
                mComplicationHandPaint);
        mComplicationHandPaint.setShadowLayer(COMPLICATION_SECONDARY_SHADOW_RADIUS, 0, 0,
                mContext.getColor(R.color.shadow_light));

        /* Draw complication on watch face's canvas */
        canvas.drawBitmap(bitmap, mTopComplicationX - width * 0.5f, mBottomComplicationY, null);
    }

    // Fires PendingIntent associated with complication (if it has one).
    void onComplicationTap(int complicationId) {
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
    int getTappedComplicationId(int touchX, int touchY) {
        ComplicationData complicationData;
        long currentTimeMillis = System.currentTimeMillis();

        for (int COMPLICATION_ID : COMPLICATION_IDS) {
            complicationData = mActiveComplicationDataSparseArray.get(COMPLICATION_ID);

            if ((complicationData != null)
                    && (complicationData.isActive(currentTimeMillis))
                    && (complicationData.getType() != ComplicationData.TYPE_NOT_CONFIGURED)
                    && (complicationData.getType() != ComplicationData.TYPE_EMPTY)) {

                float complicationX = 0;
                float complicationY = 0;
                float radius = 0;

                switch (COMPLICATION_ID) {
                    case TOP_DIAL_COMPLICATION:
                        radius = mComplicationRadius;
                        complicationX = mTopComplicationX;
                        complicationY = mTopComplicationY + radius;
                        break;

                    case LEFT_DIAL_COMPLICATION:
                        radius = (mBackgroundWidth * 0.5f * 0.35f) / 2f;
                        complicationX = mLeftComplicationX;
                        complicationY = mLeftComplicationY;
                        break;

                    case BOTTOM_DIAL_COMPLICATION:
                        radius = mComplicationRadius;
                        complicationX = mTopComplicationX;
                        complicationY = mBottomComplicationY + radius;
                        break;

                    default:
                        break;
                }

                // Distance between two points formula
                float touchRadius = (float) Math.sqrt(Math.pow(complicationX - touchX, 2f)
                        + Math.pow(complicationY - touchY, 2f));

                if (touchRadius < radius) {
                    Log.d(TAG, "getTappedComplicationId: " + COMPLICATION_ID);
                    return COMPLICATION_ID;
                } else {
                    Log.e(TAG, "Not a recognized complication id.");
                }
            }
        }
        return -1;
    }

    /**
     * Adds/updates active complication data in the array.
     *
     * @param watchFaceComplicationId id of the complication to update
     * @param data                    complication data
     */
    void updateComplicationsArray(int watchFaceComplicationId, ComplicationData data) {
        mActiveComplicationDataSparseArray.put(watchFaceComplicationId, data);
    }

    /**
     * Turns anti aliasing on and off.
     *
     * @param inAmbientMode true if device is in ambient mode
     */
    void setAmbientMode(boolean inAmbientMode) {
        mComplicationPaint.setAntiAlias(!inAmbientMode);
    }

    /**
     * Calculates positions of the complications based on watch face size.
     *
     * @param backgroundWidth  Width of the watch face
     * @param backgroundHeight Height of the watch face
     */
    void recalculateComplicationsPositions(int backgroundWidth, int backgroundHeight) {
        mBackgroundWidth = backgroundWidth;
        mBackgroundHeight = backgroundHeight;
        mComplicationRadius = mBackgroundWidth / 7f;
        mTopComplicationX = mBackgroundWidth / 2;
        mTopComplicationY = (mBackgroundHeight / 2) - (int) (2.3f * mComplicationRadius);
        mLeftComplicationX = (mBackgroundWidth / 4) + (mBackgroundWidth / 32);
        mLeftComplicationY = mBackgroundHeight / 2;
        mBottomComplicationY = (mBackgroundHeight / 2) + (int) (0.3f * mComplicationRadius);
        mTextComplicationBackground = createComplicationBackground(mComplicationRadius);
        mRangeComplicationBackground = createRangeComplicationBackground(mTextComplicationBackground);
    }

    /**
     * Adds ticks to provided complication's background.
     *
     * @param backgroundBitmap Destination bitmap
     * @return Background bitmap with ticks drawn
     */
    private Bitmap createRangeComplicationBackground(Bitmap backgroundBitmap) {
        // Get center point coordinates
        float centerX = backgroundBitmap.getWidth() * 0.5f;
        float centerY = backgroundBitmap.getHeight() * 0.5f;

        // Prepare new canvas for drawing complication
        Bitmap bitmap = Bitmap.createBitmap(backgroundBitmap);
        Canvas complicationCanvas = new Canvas(bitmap);
        /* Draw ticks */
        float primaryInnerTickRadius = centerX - 16f;
        float secondaryInnerTickRadius = centerX - 12f;
        float outerTickRadius = centerX - 8f;

        // Text for tick labels
        String[] labels = {"60", "80", "100", "0", "20", "40"};
        int labelIndex = 0;

        // Rotate the canvas so the range starts at 210 rather than 0 degrees
        complicationCanvas.rotate(CANVAS_ROTATION_DEGREES, centerX, centerY);

        int tickLimit = 21;
        for (int tickIndex = 0; tickIndex < 23; tickIndex++) {
            float tickRot = (float) (tickIndex * Math.PI * 2f / 24f);
            float primaryInnerX = (float) Math.sin(tickRot) * primaryInnerTickRadius;
            float secondaryInnerX = (float) Math.sin(tickRot) * secondaryInnerTickRadius;
            float primaryInnerY = (float) -Math.cos(tickRot) * primaryInnerTickRadius;
            float secondaryInnerY = (float) -Math.cos(tickRot) * secondaryInnerTickRadius;
            float outerX = (float) Math.sin(tickRot) * outerTickRadius;
            float outerY = (float) -Math.cos(tickRot) * outerTickRadius;

            // Labelled tick
            if (tickIndex % 2 == 0 && tickIndex % 4 != 0) {
                // Get label's text
                String label = labels[labelIndex++];

                // Calculate text's offset
                Rect textBounds = new Rect();
                mComplicationTickPaint.getTextBounds(label, 0, label.length(), textBounds);
                float textOffsetX = labelIndex == 3 ?
                        textBounds.width() * 0.75f : textBounds.width() * 0.5f;
                float textOffsetY = textBounds.height() * 0.5f;

                // Calculate label's offset
                float offsetX = centerX + secondaryInnerX - textOffsetX;
                float offsetY = centerY + secondaryInnerY + textOffsetY;

                // Rotate canvas back to original position so text is not drawn rotated
                complicationCanvas.save();
                complicationCanvas.rotate(-CANVAS_ROTATION_DEGREES, centerX, centerY);
                mComplicationTickPaint.setColor(mContext.getColor(R.color.primary_light));

                // Draw label's text
                complicationCanvas.drawText(
                        label,
                        offsetX,
                        offsetY,
                        mComplicationTickPaint);

                // Restore canvas & paint
                mComplicationTickPaint.setColor(Color.WHITE);
                complicationCanvas.restore();
            }
            if (tickIndex > 0 && tickIndex < tickLimit) {
                if (tickIndex % 2 == 0 && tickIndex % 4 != 0) {
                    // Long tick line
                    complicationCanvas.drawLine(
                            centerX + primaryInnerX,
                            centerY + primaryInnerY,
                            centerX + outerX,
                            centerY + outerY,
                            mComplicationTickPaint);
                }
                if (tickIndex % 2 != 0) {
                    // Short tick line
                    complicationCanvas.drawLine(
                            centerX + secondaryInnerX,
                            centerY + secondaryInnerY,
                            centerX + outerX,
                            centerY + outerY,
                            mComplicationPaint);
                }
            }
        }

        return bitmap;
    }

    /**
     * Creates bitmap to be used as short text complication's background.
     *
     * @param complicationRadius Radius of a circle containing complication
     * @return Complication's background bitmap
     */
    private Bitmap createComplicationBackground(float complicationRadius) {
        // Define complication's size
        int width = (int) complicationRadius * 2;
        int height = (int) complicationRadius * 2;
        float centerX = width * 0.5f;
        float centerY = height * 0.5f;
        float radius = (width * 0.5f) - 1f;

        // Prepare new canvas for drawing complication
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas complicationCanvas = new Canvas(bitmap);

        // Complication background
        complicationCanvas.drawCircle(
                centerX,
                centerY,
                radius,
                mComplicationBackgroundPaint);

        // Complication stroke
        complicationCanvas.drawCircle(
                centerX,
                centerY,
                radius,
                mComplicationStrokePaint);

        // Complication stroke
        complicationCanvas.drawCircle(
                centerX,
                centerY,
                radius + COMPLICATION_MASK_STROKE_WIDTH / 2,
                mComplicationMaskPaint);

        return bitmap;
    }
}
