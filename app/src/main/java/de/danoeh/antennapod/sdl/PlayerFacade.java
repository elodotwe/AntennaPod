package de.danoeh.antennapod.sdl;

import android.content.Context;
import android.util.Log;
import android.widget.ImageButton;

import de.danoeh.antennapod.core.event.MessageEvent;
import de.danoeh.antennapod.core.event.ProgressEvent;
import de.danoeh.antennapod.core.event.QueueEvent;
import de.danoeh.antennapod.core.event.ServiceEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.greenrobot.event.EventBus;

public class PlayerFacade {
    private static final String TAG = "PlayerFacade";

    public interface Listener {
        void onPositionUpdate(int positionMsec, int durationMsec);
        void onMediaInfoUpdate(String episodeTitle, String feedTitle);
        void onPlaybackEnd();
    }

    private Listener listener;
    private Context context;
    private PlaybackController playbackController;

    public PlayerFacade(Listener listener, Context context) {
        this.listener = listener;
        this.context = context;
        playbackController = newPlaybackController();
        playbackController.init();
        EventBus.getDefault().register(this);
    }

    // Not unused--called via Reflection by EventBus.
    @SuppressWarnings("unused")
    public void onEvent(QueueEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        // we are only interested in the number of queue items, not download status or position
//        if(event.action == QueueEvent.Action.DELETED_MEDIA ||
//                event.action == QueueEvent.Action.SORTED ||
//                event.action == QueueEvent.Action.MOVED) {
//            return;
//        }

    }

    // Not unused--called via Reflection by EventBus.
    @SuppressWarnings("unused")
    public void onEventMainThread(ServiceEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        switch(event.action) {
            case SERVICE_STARTED:
                playbackController.init();
                break;
        }
    }

    // Not unused--called via Reflection by EventBus.
    @SuppressWarnings("unused")
    public void onEventMainThread(ProgressEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        switch(event.action) {
            case START:

                break;
            case END:

                break;
        }
    }

    // Not unused--called via Reflection by EventBus.
    @SuppressWarnings("unused")
    public void onEventMainThread(MessageEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");

    }

    public void release() {
        playbackController.release();
        EventBus.getDefault().unregister(this);
    }

    public void play() {
        playbackController.playPause(PlaybackController.DesiredPlaybackOperation.PLAYING);
    }

    public void pause() {
        playbackController.playPause(PlaybackController.DesiredPlaybackOperation.PAUSED);
    }

    public void togglePlayPause() {
        playbackController.playPause(PlaybackController.DesiredPlaybackOperation.TOGGLE);
    }

    public void jumpForward() {
        playbackController.seekTo(playbackController.getPosition() + (UserPreferences.getFastForwardSecs() * 1000));
    }

    public void jumpBackward() {
        playbackController.seekTo(playbackController.getPosition() - (UserPreferences.getRewindSecs() * 1000));
    }

    public void next() {
        playPrevNext(false);
    }

    public void previous() {
        playPrevNext(true);
    }

    public boolean isPlaying() {
        return playbackController.getStatus() == PlayerStatus.PLAYING;
    }

    private void playPrevNext(boolean isPrev) {
        if (playbackController.getStatus() != PlayerStatus.PLAYING) {
            playbackController.playPause();
            return;
        }

        FeedItem item = getPrevNext(isPrev);
        if (item != null) {
            DBTasks.playMedia(context, item.getMedia(), false, true, false);
        }
    }

    private FeedItem getPrevNext(boolean isPrev) {
        if (!(playbackController.getMedia() instanceof FeedMedia)) {
            Log.i(TAG, "getPrevNext: Current media not a FeedMedia");
            return null;
        }

        FeedMedia media = (FeedMedia)playbackController.getMedia();
        if (media.getItem() == null) {
            Log.i(TAG, "getPrevNext: Current media has no item");
            return null;
        }

        FeedItem item = media.getItem();

        if (isPrev) {
            return DBTasks.getQueuePrecursorOfItem(item.getId(), null);
        } else {
            return DBTasks.getQueueSuccessorOfItem(item.getId(), null);
        }
    }

    private PlaybackController newPlaybackController() {
        return new PlaybackController(context, false) {

            @Override
            public void setupGUI() {

            }

            @Override
            public void onPositionObserverUpdate() {
                listener.onPositionUpdate(getPosition(), getDuration());
            }

            @Override
            public void onBufferStart() {

            }

            @Override
            public void onBufferEnd() {

            }

            @Override
            public void onBufferUpdate(float progress) {

            }

            @Override
            public void handleError(int code) {

            }

            @Override
            public void onReloadNotification(int code) {

            }

            @Override
            public void onSleepTimerUpdate() {

            }

            @Override
            public ImageButton getPlayButton() {
                return null;
            }

            @Override
            public void postStatusMsg(int msg, boolean showToast) {

            }

            @Override
            public void clearStatusMsg() {

            }

            @Override
            public boolean loadMediaInfo() {
                Playable playable = getMedia();
                if (playable == null) return true;

                listener.onMediaInfoUpdate(playable.getEpisodeTitle(), playable.getFeedTitle());
                onPositionObserverUpdate();
                return true;
            }

            @Override
            public void onAwaitingVideoSurface() {

            }

            @Override
            public void onServiceQueried() {

            }

            @Override
            public void onShutdownNotification() {

            }

            @Override
            public void onPlaybackEnd() {
                listener.onPlaybackEnd();
            }

            @Override
            public void onPlaybackSpeedChange() {

            }

            @Override
            protected void setScreenOn(boolean enable) {
                super.setScreenOn(enable);

            }

            @Override
            public void onSetSpeedAbilityChanged() {

            }
        };
    }
}
