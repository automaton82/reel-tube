package com.systems.automaton.reeltube.info_list.holder;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.systems.automaton.reeltube.R;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import com.systems.automaton.reeltube.info_list.InfoItemBuilder;
import com.systems.automaton.reeltube.local.history.HistoryRecordManager;
import com.systems.automaton.reeltube.util.PicassoHelper;
import com.systems.automaton.reeltube.util.Localization;

public class PlaylistMiniInfoItemHolder extends InfoItemHolder {
    public final ImageView itemThumbnailView;
    private final TextView itemStreamCountView;
    public final TextView itemTitleView;
    public final TextView itemUploaderView;

    public PlaylistMiniInfoItemHolder(final InfoItemBuilder infoItemBuilder, final int layoutId,
                                      final ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);

        itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
        itemTitleView = itemView.findViewById(R.id.itemTitleView);
        itemStreamCountView = itemView.findViewById(R.id.itemStreamCountView);
        itemUploaderView = itemView.findViewById(R.id.itemUploaderView);
    }

    public PlaylistMiniInfoItemHolder(final InfoItemBuilder infoItemBuilder,
                                      final ViewGroup parent) {
        this(infoItemBuilder, R.layout.list_playlist_mini_item, parent);
    }

    @Override
    public void updateFromItem(final InfoItem infoItem,
                               final HistoryRecordManager historyRecordManager) {
        if (!(infoItem instanceof PlaylistInfoItem)) {
            return;
        }
        final PlaylistInfoItem item = (PlaylistInfoItem) infoItem;

        itemTitleView.setText(item.getName());
        itemStreamCountView.setText(Localization
                .localizeStreamCountMini(itemStreamCountView.getContext(), item.getStreamCount()));
        itemUploaderView.setText(item.getUploaderName());

        PicassoHelper.loadPlaylistThumbnail(item.getThumbnailUrl()).into(itemThumbnailView);

        itemView.setOnClickListener(view -> {
            if (itemBuilder.getOnPlaylistSelectedListener() != null) {
                itemBuilder.getOnPlaylistSelectedListener().selected(item);
            }
        });

        itemView.setLongClickable(true);
        itemView.setOnLongClickListener(view -> {
            if (itemBuilder.getOnPlaylistSelectedListener() != null) {
                itemBuilder.getOnPlaylistSelectedListener().held(item);
            }
            return true;
        });
    }
}
