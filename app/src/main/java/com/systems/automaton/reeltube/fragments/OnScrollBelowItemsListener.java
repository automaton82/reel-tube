package com.systems.automaton.reeltube.fragments;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * Recycler view scroll listener which calls the method {@link #onScrolledDown(RecyclerView)}
 * if the view is scrolled below the last item.
 */
public abstract class OnScrollBelowItemsListener extends RecyclerView.OnScrollListener {
    @Override
    public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (dy > 0) {
            int pastVisibleItems = 0;
            final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

            final int visibleItemCount = layoutManager.getChildCount();
            final int totalItemCount = layoutManager.getItemCount();

            // Already covers the GridLayoutManager case
            if (layoutManager instanceof LinearLayoutManager) {
                pastVisibleItems = ((LinearLayoutManager) layoutManager)
                        .findFirstVisibleItemPosition();
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                final int[] positions = ((StaggeredGridLayoutManager) layoutManager)
                        .findFirstVisibleItemPositions(null);
                if (positions != null && positions.length > 0) {
                    pastVisibleItems = positions[0];
                }
            }

            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                onScrolledDown(recyclerView);
            }
        }
    }

    /**
     * Called when the recycler view is scrolled below the last item.
     *
     * @param recyclerView the recycler view
     */
    public abstract void onScrolledDown(RecyclerView recyclerView);
}
