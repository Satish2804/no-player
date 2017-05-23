package com.novoda.noplayer.exoplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.novoda.noplayer.ContentType;
import com.novoda.noplayer.listeners.InfoListeners;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ExoPlayerTwoFacade implements VideoRendererEventListener {

    private int videoWidth;
    private int videoHeight;

    private final SimpleExoPlayer exoPlayer;
    private final MediaSourceFactory mediaSourceFactory;

    private List<Listener> listeners = new CopyOnWriteArrayList<>();
    private InfoListeners infoListeners;
    private InternalErrorListener internalErrorListener;

    public static ExoPlayerTwoFacade newInstance(Context context) {
        DefaultDataSourceFactory defaultDataSourceFactory = new DefaultDataSourceFactory(context, "user-agent");
        Handler handler = new Handler(Looper.getMainLooper());
        DefaultTrackSelector trackSelector = new DefaultTrackSelector();
        SimpleExoPlayer exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector, new DefaultLoadControl());
        MediaSourceFactory mediaSourceFactory = new MediaSourceFactory(defaultDataSourceFactory, handler);
        return new ExoPlayerTwoFacade(exoPlayer, mediaSourceFactory);
    }

    public ExoPlayerTwoFacade(SimpleExoPlayer exoPlayer, MediaSourceFactory mediaSourceFactory) {
        this.exoPlayer = exoPlayer;
        this.mediaSourceFactory = mediaSourceFactory;
    }

    public boolean getPlayWhenReady() {
        if (hasPlayer()) {
            return exoPlayer.getPlayWhenReady();
        } else {
            return false;
        }
    }

    public long getPlayheadPosition() {
        if (hasPlayer()) {
            return exoPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    private boolean hasPlayer() {
        return exoPlayer != null;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public long getMediaDuration() {
        if (hasPlayer()) {
            return exoPlayer.getDuration();
        } else {
            return 0;
        }
    }

    @Override
    public void onVideoEnabled(DecoderCounters counters) {
        // no-op
    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        // no-op
    }

    @Override
    public void onVideoInputFormatChanged(Format format) {
        // no-op
    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {
        if (infoListeners == null) {
            return;
        }
        infoListeners.onDroppedFrames(count, elapsedMs);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        videoHeight = height;
        videoWidth = width;
        for (Listener listener : listeners) {
            listener.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
        }
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
        // no-op
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {
        // no-op
    }

    public int getBufferedPercentage() {
        if (hasPlayer()) {
            return exoPlayer.getBufferedPercentage();
        } else {
            return 0;
        }
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        if (hasPlayer()) {
            exoPlayer.setPlayWhenReady(playWhenReady);
        }
    }

    public void seekTo(long positionMillis) {
        if (hasPlayer()) {
            exoPlayer.seekTo(positionMillis);
        }
    }

    public void release() {
        if (hasPlayer()) {
            exoPlayer.release();
        }
    }

    public void prepare(Uri uri, ContentType contentType) {
        exoPlayer.addListener(listener);

        setPlayWhenReady(true);

        MediaSource mediaSource = mediaSourceFactory.create(contentType, uri, eventListener);
        exoPlayer.prepare(mediaSource, true, false);
    }

    private final ExoPlayer.EventListener listener = new ExoPlayer.EventListener() {

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            // no-op
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            // no-op
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            // no-op
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            for (Listener listener : listeners) {
                listener.onPlayerStateChanged(playWhenReady, playbackState);
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            for (Listener listener : listeners) {
                listener.onPlayerError(error);
            }
        }

        @Override
        public void onPositionDiscontinuity() {
            //no-op
        }
    };

    private final ExtractorMediaSource.EventListener eventListener = new ExtractorMediaSource.EventListener() {
        @Override
        public void onLoadError(IOException error) {
            internalErrorListener.onLoadError(error);
        }
    };

    public void setPlayer(SimpleExoPlayerView simpleExoPlayerView) {
        simpleExoPlayerView.setPlayer(exoPlayer);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void stop() {
        exoPlayer.stop();
    }

    public void setInfoListeners(InfoListeners infoListeners) {
        this.infoListeners = infoListeners;
    }

    public void setInternalErrorListener(InternalErrorListener internalErrorListener) {
        this.internalErrorListener = internalErrorListener;
    }

    public interface Listener {

        void onPlayerStateChanged(boolean playWhenReady, int playbackState);

        void onPlayerError(ExoPlaybackException error);

        void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio);
    }

}
