package com.systems.automaton.reeltube.player.resolver;

import static com.systems.automaton.reeltube.util.ListHelper.getNonTorrentStreams;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.source.MediaSource;

import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import com.systems.automaton.reeltube.player.helper.PlayerDataSource;
import com.systems.automaton.reeltube.player.mediaitem.MediaItemTag;
import com.systems.automaton.reeltube.player.mediaitem.StreamInfoTag;
import com.systems.automaton.reeltube.util.ListHelper;

import java.util.List;

public class AudioPlaybackResolver implements PlaybackResolver {
    private static final String TAG = AudioPlaybackResolver.class.getSimpleName();

    @NonNull
    private final Context context;
    @NonNull
    private final PlayerDataSource dataSource;

    public AudioPlaybackResolver(@NonNull final Context context,
                                 @NonNull final PlayerDataSource dataSource) {
        this.context = context;
        this.dataSource = dataSource;
    }

    @Override
    @Nullable
    public MediaSource resolve(@NonNull final StreamInfo info) {
        final MediaSource liveSource = PlaybackResolver.maybeBuildLiveMediaSource(dataSource, info);
        if (liveSource != null) {
            return liveSource;
        }

        final List<AudioStream> audioStreams = getNonTorrentStreams(info.getAudioStreams());

        final int index = ListHelper.getDefaultAudioFormat(context, audioStreams);
        if (index < 0 || index >= info.getAudioStreams().size()) {
            return null;
        }

        final AudioStream audio = info.getAudioStreams().get(index);
        final MediaItemTag tag = StreamInfoTag.of(info);

        try {
            return PlaybackResolver.buildMediaSource(
                    dataSource, audio, info, PlaybackResolver.cacheKeyOf(info, audio), tag);
        } catch (final ResolverException e) {
            Log.e(TAG, "Unable to create audio source", e);
            return null;
        }
    }
}
