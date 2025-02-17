package com.systems.automaton.reeltube.database.history.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RewriteQueriesToDropUnusedColumns;

import com.systems.automaton.reeltube.database.history.model.StreamHistoryEntity;
import com.systems.automaton.reeltube.database.history.model.StreamHistoryEntry;
import com.systems.automaton.reeltube.database.stream.StreamStatisticsEntry;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;

import static com.systems.automaton.reeltube.database.history.model.StreamHistoryEntity.JOIN_STREAM_ID;
import static com.systems.automaton.reeltube.database.history.model.StreamHistoryEntity.STREAM_ACCESS_DATE;
import static com.systems.automaton.reeltube.database.history.model.StreamHistoryEntity.STREAM_HISTORY_TABLE;
import static com.systems.automaton.reeltube.database.history.model.StreamHistoryEntity.STREAM_REPEAT_COUNT;
import static com.systems.automaton.reeltube.database.stream.StreamStatisticsEntry.STREAM_LATEST_DATE;
import static com.systems.automaton.reeltube.database.stream.StreamStatisticsEntry.STREAM_WATCH_COUNT;
import static com.systems.automaton.reeltube.database.stream.model.StreamEntity.STREAM_ID;
import static com.systems.automaton.reeltube.database.stream.model.StreamEntity.STREAM_TABLE;
import static com.systems.automaton.reeltube.database.stream.model.StreamStateEntity.JOIN_STREAM_ID_ALIAS;
import static com.systems.automaton.reeltube.database.stream.model.StreamStateEntity.STREAM_PROGRESS_MILLIS;
import static com.systems.automaton.reeltube.database.stream.model.StreamStateEntity.STREAM_STATE_TABLE;

@Dao
public abstract class StreamHistoryDAO implements HistoryDAO<StreamHistoryEntity> {
    @Query("SELECT * FROM " + STREAM_HISTORY_TABLE
            + " WHERE " + STREAM_ACCESS_DATE + " = "
            + "(SELECT MAX(" + STREAM_ACCESS_DATE + ") FROM " + STREAM_HISTORY_TABLE + ")")
    @Override
    @Nullable
    public abstract StreamHistoryEntity getLatestEntry();

    @Override
    @Query("SELECT * FROM " + STREAM_HISTORY_TABLE)
    public abstract Flowable<List<StreamHistoryEntity>> getAll();

    @Override
    @Query("DELETE FROM " + STREAM_HISTORY_TABLE)
    public abstract int deleteAll();

    @Override
    public Flowable<List<StreamHistoryEntity>> listByService(final int serviceId) {
        throw new UnsupportedOperationException();
    }

    @Query("SELECT * FROM " + STREAM_TABLE
            + " INNER JOIN " + STREAM_HISTORY_TABLE
            + " ON " + STREAM_ID + " = " + JOIN_STREAM_ID
            + " ORDER BY " + STREAM_ACCESS_DATE + " DESC")
    public abstract Flowable<List<StreamHistoryEntry>> getHistory();


    @Query("SELECT * FROM " + STREAM_TABLE
            + " INNER JOIN " + STREAM_HISTORY_TABLE
            + " ON " + STREAM_ID + " = " + JOIN_STREAM_ID
            + " ORDER BY " + STREAM_ID + " ASC")
    public abstract Flowable<List<StreamHistoryEntry>> getHistorySortedById();

    @Query("SELECT * FROM " + STREAM_HISTORY_TABLE + " WHERE " + JOIN_STREAM_ID
            + " = :streamId ORDER BY " + STREAM_ACCESS_DATE + " DESC LIMIT 1")
    @Nullable
    public abstract StreamHistoryEntity getLatestEntry(long streamId);

    @Query("DELETE FROM " + STREAM_HISTORY_TABLE + " WHERE " + JOIN_STREAM_ID + " = :streamId")
    public abstract int deleteStreamHistory(long streamId);

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM " + STREAM_TABLE

            // Select the latest entry and watch count for each stream id on history table
            + " INNER JOIN "
            + "(SELECT " + JOIN_STREAM_ID + ", "
            + "  MAX(" + STREAM_ACCESS_DATE + ") AS " + STREAM_LATEST_DATE + ", "
            + "  SUM(" + STREAM_REPEAT_COUNT + ") AS " + STREAM_WATCH_COUNT
            + " FROM " + STREAM_HISTORY_TABLE + " GROUP BY " + JOIN_STREAM_ID + ")"

            + " ON " + STREAM_ID + " = " + JOIN_STREAM_ID

            + " LEFT JOIN "
            + "(SELECT " + JOIN_STREAM_ID + " AS " + JOIN_STREAM_ID_ALIAS + ", "
            + STREAM_PROGRESS_MILLIS
            + " FROM " + STREAM_STATE_TABLE + " )"
            + " ON " + STREAM_ID + " = " + JOIN_STREAM_ID_ALIAS)
    public abstract Flowable<List<StreamStatisticsEntry>> getStatistics();
}
