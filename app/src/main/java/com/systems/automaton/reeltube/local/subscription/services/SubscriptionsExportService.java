/*
 * Copyright 2018 Mauricio Colli <mauriciocolli@outlook.com>
 * SubscriptionsExportService.java is part of NewPipe
 *
 * License: GPL-3.0+
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.systems.automaton.reeltube.local.subscription.services;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import com.systems.automaton.reeltube.App;
import com.systems.automaton.reeltube.R;
import com.systems.automaton.reeltube.database.subscription.SubscriptionEntity;
import org.schabi.newpipe.extractor.subscription.SubscriptionItem;
import com.systems.automaton.reeltube.streams.io.SharpOutputStream;
import com.systems.automaton.reeltube.streams.io.StoredFileHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static com.systems.automaton.reeltube.MainActivity.DEBUG;

public class SubscriptionsExportService extends BaseImportExportService {
    public static final String KEY_FILE_PATH = "key_file_path";

    /**
     * A {@link LocalBroadcastManager local broadcast} will be made with this action
     * when the export is successfully completed.
     */
    public static final String EXPORT_COMPLETE_ACTION = App.PACKAGE_NAME + ".local.subscription"
            + ".services.SubscriptionsExportService.EXPORT_COMPLETE";

    private Subscription subscription;
    private StoredFileHelper outFile;
    private OutputStream outputStream;

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent == null || subscription != null) {
            return START_NOT_STICKY;
        }

        final Uri path = intent.getParcelableExtra(KEY_FILE_PATH);
        if (path == null) {
            stopAndReportError(new IllegalStateException(
                    "Exporting to a file, but the path is null"),
                    "Exporting subscriptions");
            return START_NOT_STICKY;
        }

        try {
            outFile = new StoredFileHelper(this, path, "application/json");
            outputStream = new SharpOutputStream(outFile.getStream());
        } catch (final IOException e) {
            handleError(e);
            return START_NOT_STICKY;
        }

        startExport();

        return START_NOT_STICKY;
    }

    @Override
    protected int getNotificationId() {
        return 4567;
    }

    @Override
    public int getTitle() {
        return R.string.export_ongoing;
    }

    @Override
    protected void disposeAll() {
        super.disposeAll();
        if (subscription != null) {
            subscription.cancel();
        }
    }

    private void startExport() {
        showToast(R.string.export_ongoing);

        subscriptionManager.subscriptionTable().getAll().take(1)
                .map(subscriptionEntities -> {
                    final List<SubscriptionItem> result
                            = new ArrayList<>(subscriptionEntities.size());
                    for (final SubscriptionEntity entity : subscriptionEntities) {
                        result.add(new SubscriptionItem(entity.getServiceId(), entity.getUrl(),
                                entity.getName()));
                    }
                    return result;
                })
                .map(exportToFile())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getSubscriber());
    }

    private Subscriber<StoredFileHelper> getSubscriber() {
        return new Subscriber<StoredFileHelper>() {
            @Override
            public void onSubscribe(final Subscription s) {
                subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(final StoredFileHelper file) {
                if (DEBUG) {
                    Log.d(TAG, "startExport() success: file = " + file);
                }
            }

            @Override
            public void onError(final Throwable error) {
                Log.e(TAG, "onError() called with: error = [" + error + "]", error);
                handleError(error);
            }

            @Override
            public void onComplete() {
                LocalBroadcastManager.getInstance(SubscriptionsExportService.this)
                        .sendBroadcast(new Intent(EXPORT_COMPLETE_ACTION));
                showToast(R.string.export_complete_toast);
                stopService();
            }
        };
    }

    private Function<List<SubscriptionItem>, StoredFileHelper> exportToFile() {
        return subscriptionItems -> {
            ImportExportJsonHelper.writeTo(subscriptionItems, outputStream, eventListener);
            return outFile;
        };
    }

    protected void handleError(final Throwable error) {
        super.handleError(R.string.subscriptions_export_unsuccessful, error);
    }
}
