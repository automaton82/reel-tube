package com.systems.automaton.reeltube.local.holder;

import android.view.ViewGroup;

import com.systems.automaton.reeltube.R;
import com.systems.automaton.reeltube.local.LocalItemBuilder;

public class LocalStatisticStreamGridItemHolder extends LocalStatisticStreamItemHolder {
    public LocalStatisticStreamGridItemHolder(final LocalItemBuilder infoItemBuilder,
                                              final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_stream_grid_item, parent);
    }
}
