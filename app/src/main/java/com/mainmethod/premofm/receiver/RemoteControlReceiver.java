/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import com.mainmethod.premofm.service.PodcastPlayerService;

/**
 * Broadcast receiver for starting the media player when a media button is pressed
 * Created by evan on 9/26/15.
 */
public class RemoteControlReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    PodcastPlayerService.sendIntent(context, PodcastPlayerService.ACTION_RESUME_PLAYBACK, -1);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    PodcastPlayerService.sendIntent(context, PodcastPlayerService.ACTION_PAUSE, -1);
                    break;
                case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
                    PodcastPlayerService.sendIntent(context, PodcastPlayerService.ACTION_SEEK_FORWARD, -1);
                    break;
                case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
                    PodcastPlayerService.sendIntent(context, PodcastPlayerService.ACTION_SEEK_BACKWARD, -1);
                    break;
            }
        }
    }
}
