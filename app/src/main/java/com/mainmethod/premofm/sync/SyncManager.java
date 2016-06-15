package com.mainmethod.premofm.sync;

import android.content.Context;

import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.object.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Created by evanhalley on 6/14/16.
 */
public class SyncManager implements Runnable {

    private static final int MAX_RUNTIME_MIN = 30;

    private final Context context;
    private List<Channel> channelList;
    private final ExecutorService executor;
    private final boolean doNotify;

    public SyncManager(Context context, Channel channel) {
        this.context = context;
        this.executor = Executors.newFixedThreadPool(1, new SyncThreadFactory());
        this.channelList = new ArrayList<>(1);
        channelList.add(channel);
        doNotify = false;
    }

    public SyncManager(Context context, boolean doNotify) {
        this.context = context;
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                new SyncThreadFactory());
        this.doNotify = doNotify;
    }

    @Override
    public void run() {

        if (channelList == null) {
            // TODO build getChannelsToSync to sort and sync by channel data like last sync time
            channelList = ChannelModel.getChannels(context);
        }
        Timber.d("Starting sync manager with %d channels", channelList.size());

        for (int i = 0; i < channelList.size(); i++) {
            executor.execute(new SyncWorker(context, channelList.get(i), doNotify));
        }

        try {
            executor.shutdown();
            boolean finished = executor.awaitTermination(MAX_RUNTIME_MIN, TimeUnit.MINUTES);

            if (!finished) {
                Timber.d("Executer service timed out");
            } else {
                Timber.d("Executor service finished execution");
            }
        } catch (InterruptedException e) {
           Timber.e(e, "Error in run");
        }
    }

    private static class SyncThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }
}
