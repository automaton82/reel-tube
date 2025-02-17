package com.systems.automaton.reeltube.player.event;

import com.google.android.exoplayer2.PlaybackException;

public interface PlayerServiceEventListener extends PlayerEventListener {
    void onFullscreenStateChanged(boolean fullscreen);

    void onScreenRotationButtonClicked();

    void onMoreOptionsLongClicked();

    void onPlayerError(PlaybackException error, boolean isCatchableException);

    void hideSystemUiIfNeeded();
}
