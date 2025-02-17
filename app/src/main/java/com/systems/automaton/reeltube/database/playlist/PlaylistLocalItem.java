package com.systems.automaton.reeltube.database.playlist;

import com.systems.automaton.reeltube.database.LocalItem;
import com.systems.automaton.reeltube.database.playlist.model.PlaylistRemoteEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public interface PlaylistLocalItem extends LocalItem {
    String getOrderingName();

    static List<PlaylistLocalItem> merge(
            final List<PlaylistMetadataEntry> localPlaylists,
            final List<PlaylistRemoteEntity> remotePlaylists) {
        final List<PlaylistLocalItem> items = new ArrayList<>(
                localPlaylists.size() + remotePlaylists.size());
        items.addAll(localPlaylists);
        items.addAll(remotePlaylists);

        Collections.sort(items, Comparator.comparing(PlaylistLocalItem::getOrderingName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        return items;
    }
}
