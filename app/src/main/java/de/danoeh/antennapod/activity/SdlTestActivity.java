package de.danoeh.antennapod.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.sdl.PlayerFacade;


public class SdlTestActivity extends AppCompatActivity implements PlayerFacade.Listener {

    PlayerFacade playerFacade = null;
    TextView feedTitle = null;
    TextView episodeTitle = null;
    TextView position = null;
    TextView duration = null;
    Spinner feedSpinner = null;
    Spinner episodeSpinner = null;
    Feed selectedFeed = null;


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
        feedSpinner = findViewById(R.id.feedList);
        episodeSpinner = findViewById(R.id.itemList);


        final String NOT_SET = "(not set yet)";

        feedTitle.setText(NOT_SET);
        episodeTitle.setText(NOT_SET);
        position.setText(NOT_SET);
        duration.setText(NOT_SET);

        playerFacade = new PlayerFacade(this, this);

        List<String> feedList = new ArrayList<>();
        for (Feed feed : playerFacade.getFeeds()) {
            feedList.add(feed.getTitle());
        }
        ArrayAdapter<String> feedListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, feedList);
        feedSpinner.setAdapter(feedListAdapter);
        feedListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        List<String> episodeList = new ArrayList<>();
        ArrayAdapter<String> episodeListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, episodeList);
        episodeSpinner.setAdapter(episodeListAdapter);
        episodeListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);



        feedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFeed = playerFacade.getFeeds().get(position);
                episodeListAdapter.notifyDataSetInvalidated();
                episodeList.clear();
                for (FeedItem item : playerFacade.getItemsForFeed(selectedFeed)) {
                    String title = item.getTitle();
                    if (item.isNew()) title += " (new)";
                    if (item.hasMedia()) {
                        if (item.getMedia().isDownloaded()) {
                            title += " (dwn)";
                        } else {
                            title += " (str)";
                        }
                    }

                    episodeList.add(title);

                }
                episodeListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        episodeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                FeedItem item = playerFacade.getItemsForFeed(selectedFeed).get(position);
                DBTasks.playMedia(SdlTestActivity.this, item.getMedia(), false, true, false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
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
