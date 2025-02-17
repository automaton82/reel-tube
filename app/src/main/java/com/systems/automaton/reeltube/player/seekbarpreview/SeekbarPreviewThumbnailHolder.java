package com.systems.automaton.reeltube.player.seekbarpreview;

import static com.systems.automaton.reeltube.player.seekbarpreview.SeekbarPreviewThumbnailHelper.SeekbarPreviewThumbnailType;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Stopwatch;

import org.schabi.newpipe.extractor.stream.Frameset;
import com.systems.automaton.reeltube.util.PicassoHelper;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class SeekbarPreviewThumbnailHolder {

    // This has to be <= 23 chars on devices running Android 7 or lower (API <= 25)
    // or it fails with an IllegalArgumentException
    // https://stackoverflow.com/a/54744028
    public static final String TAG = "SeekbarPrevThumbHolder";

    // Key = Position of the picture in milliseconds
    // Supplier = Supplies the bitmap for that position
    private final Map<Integer, Supplier<Bitmap>> seekbarPreviewData = new ConcurrentHashMap<>();

    // This ensures that if the reset is still undergoing
    // and another reset starts, only the last reset is processed
    private UUID currentUpdateRequestIdentifier = UUID.randomUUID();

    public synchronized void resetFrom(
            @NonNull final Context context,
            final List<Frameset> framesets) {

        final int seekbarPreviewType =
                SeekbarPreviewThumbnailHelper.getSeekbarPreviewThumbnailType(context);

        final UUID updateRequestIdentifier = UUID.randomUUID();
        this.currentUpdateRequestIdentifier = updateRequestIdentifier;

        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                resetFromAsync(seekbarPreviewType, framesets, updateRequestIdentifier);
            } catch (final Exception ex) {
                Log.e(TAG, "Failed to execute async", ex);
            }
        });
        // ensure that the executorService stops/destroys it's threads
        // after the task is finished
        executorService.shutdown();
    }

    private void resetFromAsync(
            final int seekbarPreviewType,
            final List<Frameset> framesets,
            final UUID updateRequestIdentifier) {

        Log.d(TAG, "Clearing seekbarPreviewData");
        seekbarPreviewData.clear();

        if (seekbarPreviewType == SeekbarPreviewThumbnailType.NONE) {
            Log.d(TAG, "Not processing seekbarPreviewData due to settings");
            return;
        }

        final Frameset frameset = getFrameSetForType(framesets, seekbarPreviewType);
        if (frameset == null) {
            Log.d(TAG, "No frameset was found to fill seekbarPreviewData");
            return;
        }

        Log.d(TAG, "Frameset quality info: "
                + "[width=" + frameset.getFrameWidth()
                + ", heigh=" + frameset.getFrameHeight() + "]");

        // Abort method execution if we are not the latest request
        if (!isRequestIdentifierCurrent(updateRequestIdentifier)) {
            return;
        }

        generateDataFrom(frameset, updateRequestIdentifier);
    }

    private Frameset getFrameSetForType(
            final List<Frameset> framesets,
            final int seekbarPreviewType) {

        if (seekbarPreviewType == SeekbarPreviewThumbnailType.HIGH_QUALITY) {
            Log.d(TAG, "Strategy for seekbarPreviewData: high quality");
            return framesets.stream()
                    .max(Comparator.comparingInt(fs -> fs.getFrameHeight() * fs.getFrameWidth()))
                    .orElse(null);
        } else {
            Log.d(TAG, "Strategy for seekbarPreviewData: low quality");
            return framesets.stream()
                    .min(Comparator.comparingInt(fs -> fs.getFrameHeight() * fs.getFrameWidth()))
                    .orElse(null);
        }
    }

    private void generateDataFrom(
            final Frameset frameset,
            final UUID updateRequestIdentifier) {

        Log.d(TAG, "Starting generation of seekbarPreviewData");
        final Stopwatch sw = Log.isLoggable(TAG, Log.DEBUG) ? Stopwatch.createStarted() : null;

        int currentPosMs = 0;
        int pos = 1;

        final int frameCountPerUrl = frameset.getFramesPerPageX() * frameset.getFramesPerPageY();

        // Process each url in the frameset
        for (final String url : frameset.getUrls()) {
            // get the bitmap
            final Bitmap srcBitMap = getBitMapFrom(url);

            // The data is not added directly to "seekbarPreviewData" due to
            // concurrency and checks for "updateRequestIdentifier"
            final Map<Integer, Supplier<Bitmap>> generatedDataForUrl = new HashMap<>();

            // The bitmap consists of several images, which we process here
            // foreach frame in the returned bitmap
            for (int i = 0; i < frameCountPerUrl; i++) {
                // Frames outside the video length are skipped
                if (pos > frameset.getTotalCount()) {
                    break;
                }

                // Get the bounds where the frame is found
                final int[] bounds = frameset.getFrameBoundsAt(currentPosMs);
                generatedDataForUrl.put(currentPosMs, () -> {
                    // It can happen, that the original bitmap could not be downloaded
                    // In such a case - we don't want a NullPointer - simply return null
                    if (srcBitMap == null) {
                        return null;
                    }

                    // Cut out the corresponding bitmap form the "srcBitMap"
                    return Bitmap.createBitmap(srcBitMap, bounds[1], bounds[2],
                            frameset.getFrameWidth(), frameset.getFrameHeight());
                });

                currentPosMs += frameset.getDurationPerFrame();
                pos++;
            }

            // Check if we are still the latest request
            // If not abort method execution
            if (isRequestIdentifierCurrent(updateRequestIdentifier)) {
                seekbarPreviewData.putAll(generatedDataForUrl);
            } else {
                Log.d(TAG, "Aborted of generation of seekbarPreviewData");
                break;
            }
        }

        if (sw != null) {
            Log.d(TAG, "Generation of seekbarPreviewData took " + sw.stop().toString());
        }
    }

    @Nullable
    private Bitmap getBitMapFrom(final String url) {
        if (url == null) {
            Log.w(TAG, "url is null; This should never happen");
            return null;
        }

        final Stopwatch sw = Log.isLoggable(TAG, Log.DEBUG) ? Stopwatch.createStarted() : null;
        try {
            Log.d(TAG, "Downloading bitmap for seekbarPreview from '" + url + "'");

            // Gets the bitmap within the timeout of 15 seconds imposed by default by OkHttpClient
            // Ensure that your are not running on the main-Thread this will otherwise hang
            final Bitmap bitmap = PicassoHelper.loadSeekbarThumbnailPreview(url).get();

            if (sw != null) {
                Log.d(TAG,
                        "Download of bitmap for seekbarPreview from '" + url
                                + "' took " + sw.stop().toString());
            }

            return bitmap;
        } catch (final Exception ex) {
            Log.w(TAG,
                    "Failed to get bitmap for seekbarPreview from url='" + url
                            + "' in time",
                    ex);
            return null;
        }
    }

    private boolean isRequestIdentifierCurrent(final UUID requestIdentifier) {
        return this.currentUpdateRequestIdentifier.equals(requestIdentifier);
    }


    public Optional<Bitmap> getBitmapAt(final int positionInMs) {
        // Check if the BitmapData is empty
        if (seekbarPreviewData.isEmpty()) {
            return Optional.empty();
        }

        // Get the closest frame to the requested position
        final int closestIndexPosition =
                seekbarPreviewData.keySet().stream()
                        .min(Comparator.comparingInt(i -> Math.abs(i - positionInMs)))
                        .orElse(-1);

        // this should never happen, because
        // it indicates that "seekbarPreviewData" is empty which was already checked
        if (closestIndexPosition == -1) {
            return Optional.empty();
        }

        try {
            // Get the bitmap for the position (executes the supplier)
            return Optional.ofNullable(seekbarPreviewData.get(closestIndexPosition).get());
        } catch (final Exception ex) {
            // If there is an error, log it and return Optional.empty
            Log.w(TAG, "Unable to get seekbar preview", ex);
            return Optional.empty();
        }
    }
}
