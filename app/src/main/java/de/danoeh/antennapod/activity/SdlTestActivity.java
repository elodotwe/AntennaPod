package de.danoeh.antennapod.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.sdl.PlayerFacade;


public class SdlTestActivity extends AppCompatActivity implements PlayerFacade.Listener {

    PlayerFacade playerFacade = null;
    TextView feedTitle = null;
    TextView episodeTitle = null;
    TextView position = null;
    TextView duration = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdl_test);


    }

    @Override
    protected void onResume() {
        super.onResume();

        feedTitle = findViewById(R.id.feedTitle);
        episodeTitle = findViewById(R.id.episodeTitle);
        position = findViewById(R.id.lastPosition);
        duration = findViewById(R.id.lastDuration);

        final String NOT_SET = "(not set yet)";

        feedTitle.setText(NOT_SET);
        episodeTitle.setText(NOT_SET);
        position.setText(NOT_SET);
        duration.setText(NOT_SET);

        playerFacade = new PlayerFacade(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        playerFacade.release();
        playerFacade = null;
    }

    private String msecToText(int msec) {
        return TimeUnit.MILLISECONDS.toHours(msec) + ":" +
                (TimeUnit.MILLISECONDS.toMinutes(msec) % 60) + ":" +
                (TimeUnit.MILLISECONDS.toSeconds(msec) % 60);
    }

    @Override
    public void onPositionUpdate(int positionMsec, int durationMsec) {
        runOnUiThread(() -> {
            position.setText(msecToText(positionMsec));
            duration.setText(msecToText(durationMsec));
        });
    }

    @Override
    public void onMediaInfoUpdate(String episodeTitle, String feedTitle) {
        runOnUiThread(() -> {
            this.episodeTitle.setText(episodeTitle);
            this.feedTitle.setText(feedTitle);
        });
    }

    @Override
    public void onPlaybackEnd() {
        runOnUiThread(() -> {
            final String ENDED = "Ended";
            this.episodeTitle.setText(ENDED);
            this.feedTitle.setText(ENDED);
            this.position.setText(ENDED);
            this.duration.setText(ENDED);
        });
    }

    public void onPrevious(View v) {
        playerFacade.jumpBackward();
    }

    public void onPlay(View v) {
        playerFacade.play();
    }

    public void onPause(View v) {
        playerFacade.pause();
    }

    public void onNext(View v) {
        playerFacade.jumpForward();
    }

    public void onPreviousTrack(View v) {
        playerFacade.previous();
    }

    public void  onNextTrack(View v) {
        playerFacade.next();
    }
}
