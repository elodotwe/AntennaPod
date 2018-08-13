package de.danoeh.antennapod.sdl;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.RPCStruct;
import com.smartdevicelink.proxy.SdlProxyALM;
import com.smartdevicelink.proxy.callbacks.OnServiceEnded;
import com.smartdevicelink.proxy.callbacks.OnServiceNACKed;
import com.smartdevicelink.proxy.interfaces.IProxyListenerALM;
import com.smartdevicelink.proxy.rpc.AddCommandResponse;
import com.smartdevicelink.proxy.rpc.AddSubMenuResponse;
import com.smartdevicelink.proxy.rpc.AlertManeuverResponse;
import com.smartdevicelink.proxy.rpc.AlertResponse;
import com.smartdevicelink.proxy.rpc.ButtonPressResponse;
import com.smartdevicelink.proxy.rpc.ChangeRegistrationResponse;
import com.smartdevicelink.proxy.rpc.Choice;
import com.smartdevicelink.proxy.rpc.CreateInteractionChoiceSet;
import com.smartdevicelink.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.smartdevicelink.proxy.rpc.DeleteCommandResponse;
import com.smartdevicelink.proxy.rpc.DeleteFileResponse;
import com.smartdevicelink.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.smartdevicelink.proxy.rpc.DeleteSubMenuResponse;
import com.smartdevicelink.proxy.rpc.DiagnosticMessageResponse;
import com.smartdevicelink.proxy.rpc.DialNumberResponse;
import com.smartdevicelink.proxy.rpc.EndAudioPassThruResponse;
import com.smartdevicelink.proxy.rpc.GenericResponse;
import com.smartdevicelink.proxy.rpc.GetDTCsResponse;
import com.smartdevicelink.proxy.rpc.GetInteriorVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.GetSystemCapabilityResponse;
import com.smartdevicelink.proxy.rpc.GetVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.GetWayPointsResponse;
import com.smartdevicelink.proxy.rpc.ListFilesResponse;
import com.smartdevicelink.proxy.rpc.OnAudioPassThru;
import com.smartdevicelink.proxy.rpc.OnButtonEvent;
import com.smartdevicelink.proxy.rpc.OnButtonPress;
import com.smartdevicelink.proxy.rpc.OnCommand;
import com.smartdevicelink.proxy.rpc.OnDriverDistraction;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.OnHashChange;
import com.smartdevicelink.proxy.rpc.OnInteriorVehicleData;
import com.smartdevicelink.proxy.rpc.OnKeyboardInput;
import com.smartdevicelink.proxy.rpc.OnLanguageChange;
import com.smartdevicelink.proxy.rpc.OnLockScreenStatus;
import com.smartdevicelink.proxy.rpc.OnPermissionsChange;
import com.smartdevicelink.proxy.rpc.OnStreamRPC;
import com.smartdevicelink.proxy.rpc.OnSystemRequest;
import com.smartdevicelink.proxy.rpc.OnTBTClientState;
import com.smartdevicelink.proxy.rpc.OnTouchEvent;
import com.smartdevicelink.proxy.rpc.OnVehicleData;
import com.smartdevicelink.proxy.rpc.OnWayPointChange;
import com.smartdevicelink.proxy.rpc.PerformAudioPassThruResponse;
import com.smartdevicelink.proxy.rpc.PerformInteraction;
import com.smartdevicelink.proxy.rpc.PerformInteractionResponse;
import com.smartdevicelink.proxy.rpc.PutFile;
import com.smartdevicelink.proxy.rpc.PutFileResponse;
import com.smartdevicelink.proxy.rpc.ReadDIDResponse;
import com.smartdevicelink.proxy.rpc.ResetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.ScrollableMessageResponse;
import com.smartdevicelink.proxy.rpc.SendHapticDataResponse;
import com.smartdevicelink.proxy.rpc.SendLocationResponse;
import com.smartdevicelink.proxy.rpc.SetAppIcon;
import com.smartdevicelink.proxy.rpc.SetAppIconResponse;
import com.smartdevicelink.proxy.rpc.SetDisplayLayoutResponse;
import com.smartdevicelink.proxy.rpc.SetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.SetInteriorVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.SetMediaClockTimer;
import com.smartdevicelink.proxy.rpc.SetMediaClockTimerResponse;
import com.smartdevicelink.proxy.rpc.Show;
import com.smartdevicelink.proxy.rpc.ShowConstantTbtResponse;
import com.smartdevicelink.proxy.rpc.ShowResponse;
import com.smartdevicelink.proxy.rpc.SliderResponse;
import com.smartdevicelink.proxy.rpc.SoftButton;
import com.smartdevicelink.proxy.rpc.SpeakResponse;
import com.smartdevicelink.proxy.rpc.StartTime;
import com.smartdevicelink.proxy.rpc.StreamRPCResponse;
import com.smartdevicelink.proxy.rpc.SubscribeButton;
import com.smartdevicelink.proxy.rpc.SubscribeButtonResponse;
import com.smartdevicelink.proxy.rpc.SubscribeVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.SubscribeWayPointsResponse;
import com.smartdevicelink.proxy.rpc.SystemRequestResponse;
import com.smartdevicelink.proxy.rpc.UnsubscribeButtonResponse;
import com.smartdevicelink.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.UnsubscribeWayPointsResponse;
import com.smartdevicelink.proxy.rpc.UpdateTurnListResponse;
import com.smartdevicelink.proxy.rpc.enums.AudioStreamingState;
import com.smartdevicelink.proxy.rpc.enums.ButtonName;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.InteractionMode;
import com.smartdevicelink.proxy.rpc.enums.LayoutMode;
import com.smartdevicelink.proxy.rpc.enums.SdlDisconnectedReason;
import com.smartdevicelink.proxy.rpc.enums.SoftButtonType;
import com.smartdevicelink.proxy.rpc.enums.UpdateMode;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.transport.TransportConstants;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBTasks;

public class SdlService extends Service implements IProxyListenerALM, PlayerFacade.Listener {
    private final static int NOTIFICATION_ID = R.id.notification_sdl_persistent;
    private final static String NOTIF_CHANNEL_ID = "sdl_notif";
    private final static String TAG = "SdlService";
    private static final int NEXT_SOFTBUTTON_ID = 1;
    private static final int PREV_SOFTBUTTON_ID = 2;
    private static final int LIST_SOFTBUTTON_ID = 3;
    private static final int FEEDS_CHOICESET_ID = 1;
    //Further choiceset IDs are equal to the Feed's index in the feed list + 2.
    private static final int FEED_CHOICESET_ID_BASE = 2;

    private SparseArray<FeedItem> feedItemChoiceIDs = new SparseArray<>();
    private int nextChoiceID = 0;

    //The proxy handles communication between the application and SDL
    private SdlProxyALM proxy = null;

    private boolean isFirstRun = true;

    private PlayerFacade playerFacade = null;
    private boolean newlyRegistered = true;
    private boolean isInterrupted = false;

    private String serializeOrFart(RPCStruct struct) {
        try {
            return struct.serializeJSON().toString(2);
        } catch (JSONException e) {
            return "Could not serialize struct.";
        }
    }

    private void sendRPCDammit(RPCRequest rpcRequest) {
        Log.d(TAG, "sendRPCDammit: Sending " + serializeOrFart(rpcRequest));
        try {
            proxy.sendRPCRequest(rpcRequest);
        } catch (SdlException|NullPointerException e) {
            Log.e(TAG, "sendRPCDammit: Couldn't send request...", e);
        }
    }

    private StartTime toStartTime(int msec) {
        StartTime startTime = new StartTime();
        startTime.setHours((int)TimeUnit.MILLISECONDS.toHours(msec));
        startTime.setMinutes((int)TimeUnit.MILLISECONDS.toMinutes(msec) % 60);
        startTime.setSeconds((int)TimeUnit.MILLISECONDS.toSeconds(msec) % 60);
        return startTime;
    }

    private String toHumanTime(int msec) {
        long h = TimeUnit.MILLISECONDS.toHours(msec);
        long m = TimeUnit.MILLISECONDS.toMinutes(msec) % 60;
        long s = TimeUnit.MILLISECONDS.toSeconds(msec) % 60;

        return String.format(Locale.ENGLISH, "%d:%02d:%02d", h,m,s);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        //Dispose of the proxy
        if (proxy != null) {
            try {
                proxy.dispose();
            } catch (SdlException e) {
                Log.e(TAG, "onDestroy: Failed to dispose proxy", e);
            } finally {
                proxy = null;
            }
        }

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if(notificationManager!=null){ //If this is the only notification on your channel
                notificationManager.deleteNotificationChannel(NOTIF_CHANNEL_ID);
            }
            stopForeground(true);
        }

        playerFacade.pause();

        if (playerFacade != null) {
            playerFacade.release();
        }
        playerFacade = null;

        super.onDestroy();
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) return;

            NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID, "SDL", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows when connecting/connected to your car");
            notificationManager.createNotificationChannel(channel);
            Notification serviceNotification = new Notification.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("AntennaPod SDL")
            .setContentText("AntenaPod is mayhaps connecting to your car.")
                    .setChannelId(NOTIF_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .build();
            startForeground(NOTIFICATION_ID, serviceNotification);
        }

        playerFacade = new PlayerFacade(this, this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void subscribeButton(ButtonName buttonName) {
        SubscribeButton subscribeButton = new SubscribeButton();
        subscribeButton.setButtonName(buttonName);
        try {
            proxy.sendRPCRequest(subscribeButton);
        } catch (SdlException e) {
            Log.e(TAG, "subscribeButton: Unable to subscribe to button " + buttonName.name(), e);
        }
    }

    /**
     * Helper method to take resource files and turn them into byte arrays
     * @param resource Resource file id.
     * @return Resulting byte array.
     */
    private byte[] contentsOfResource(int resource) {
        InputStream is = null;
        try {
            is = getResources().openRawResource(resource);
            ByteArrayOutputStream os = new ByteArrayOutputStream(is.available());
            final int bufferSize = 4096;
            final byte[] buffer = new byte[bufferSize];
            int available;
            while ((available = is.read(buffer)) >= 0) {
                os.write(buffer, 0, available);
            }
            return os.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, "Can't read icon file", e);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onOnHMIStatus(OnHMIStatus notification) {
        Log.d(TAG, "onOnHMIStatus() called with: notification = [" + serializeOrFart(notification) + "]");

        if (newlyRegistered) {
            PutFile putFile = new PutFile();
            putFile.setFileType(FileType.GRAPHIC_PNG);
            putFile.setSdlFileName("logo.png");
            putFile.setOnRPCResponseListener(new OnRPCResponseListener() {
                @Override
                public void onResponse(int correlationId, RPCResponse response) {
                    SetAppIcon setAppIcon = new SetAppIcon();
                    setAppIcon.setSdlFileName("logo.png");
                    sendRPCDammit(setAppIcon);
                }
            });
            putFile.setFileData(contentsOfResource(R.raw.ic_launcher));
            sendRPCDammit(putFile);

            newlyRegistered = false;
        }

        if (notification.getHmiLevel() == HMILevel.HMI_FULL) {
            if (isFirstRun) {
                subscribeButton(ButtonName.OK);
                subscribeButton(ButtonName.SEEKLEFT);
                subscribeButton(ButtonName.SEEKRIGHT);

                // Start playback if it isn't already. User expects audio to start when we are
                // foregrounded.
                playerFacade.play();

                Show show = new Show();
                show.setMainField1("Loading...");
                List<SoftButton> softButtons = new ArrayList<>();
                SoftButton softButton = new SoftButton();
                softButton.setText("Prev");
                softButton.setSoftButtonID(PREV_SOFTBUTTON_ID);
                softButton.setType(SoftButtonType.SBT_TEXT);
                softButtons.add(softButton);
                softButton = new SoftButton();
                softButton.setText("Next");
                softButton.setSoftButtonID(NEXT_SOFTBUTTON_ID);
                softButton.setType(SoftButtonType.SBT_TEXT);
                softButtons.add(softButton);

                softButton = new SoftButton();
                softButton.setType(SoftButtonType.SBT_TEXT);
                softButton.setSoftButtonID(LIST_SOFTBUTTON_ID);
                softButton.setText("List");
                softButtons.add(softButton);

                show.setSoftButtons(softButtons);
                sendRPCDammit(show);

                List<Feed> feedList = playerFacade.getFeeds();

                CreateInteractionChoiceSet createChoiceSet = new CreateInteractionChoiceSet();
                List<Choice> choices = new ArrayList<>();
                for (Feed feed : feedList) {
                    Choice choice = new Choice();
                    choice.setChoiceID(nextChoiceID);
                    nextChoiceID++;
                    choice.setMenuName(feed.getTitle());
                    choice.setSecondaryText(feed.getLastUpdate());
                    choices.add(choice);
                }
                createChoiceSet.setChoiceSet(choices);
                createChoiceSet.setInteractionChoiceSetID(FEEDS_CHOICESET_ID);
                sendRPCDammit(createChoiceSet);

                int setID = FEED_CHOICESET_ID_BASE;
                for (Feed feed : feedList) {
                    createChoiceSet = new CreateInteractionChoiceSet();
                    createChoiceSet.setInteractionChoiceSetID(setID);
                    setID++;
                    choices = new ArrayList<>();
                    for (FeedItem feedItem : playerFacade.getItemsForFeed(feed)) {
                        Choice choice = new Choice();
                        choice.setChoiceID(nextChoiceID);
                        feedItemChoiceIDs.append(nextChoiceID, feedItem);
                        nextChoiceID++;
                        choice.setMenuName(feedItem.getTitle());
                        choice.setSecondaryText(feedItem.getDescription());
                        if (feedItem.getMedia() != null) {
                            choice.setTertiaryText(toHumanTime(feedItem.getMedia().getDuration()));
                        }
                        choices.add(choice);
                    }

                    createChoiceSet.setChoiceSet(choices);
                    sendRPCDammit(createChoiceSet);
                }

                isFirstRun = false;
            }
        }

        if (notification.getAudioStreamingState().equals(AudioStreamingState.NOT_AUDIBLE)) {
            if (!isInterrupted && playerFacade.isPlaying()) {
                Log.i(TAG, "onOnHMIStatus: Not audible but was playing. Pausing.");
                isInterrupted = true;
                playerFacade.pause();
            }
        } else if (isInterrupted) {
            Log.i(TAG, "onOnHMIStatus: Resuming audio after interruption.");
            isInterrupted = false;
            playerFacade.play();
        }
    }

    @Override
    public void onProxyClosed(String info, Exception e, SdlDisconnectedReason reason) {
        Log.e(TAG, "onProxyClosed; spinning down service. info = " + info + "; reason = " + reason.name(), e);
        stopSelf();
        if(reason.equals(SdlDisconnectedReason.LANGUAGE_CHANGE)){
            Log.i(TAG, "onProxyClosed: Language change detected, restarting SdlService");
            Intent intent = new Intent(TransportConstants.START_ROUTER_SERVICE_ACTION);
            intent.putExtra(SdlReceiver.RECONNECT_LANG_CHANGE, true);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onServiceEnded(OnServiceEnded serviceEnded) {
        Log.d(TAG, "onServiceEnded() called with: serviceEnded = [" + serviceEnded + "]");
    }

    @Override
    public void onServiceNACKed(OnServiceNACKed serviceNACKed) {
        Log.d(TAG, "onServiceNACKed() called with: serviceNACKed = [" + serviceNACKed + "]");
    }

    @Override
    public void onOnStreamRPC(OnStreamRPC notification) {
        Log.d(TAG, "onOnStreamRPC() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onStreamRPCResponse(StreamRPCResponse response) {
        Log.d(TAG, "onStreamRPCResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onError(String info, Exception e) {
        Log.e(TAG, "onError: info=" + info, e);
    }

    @Override
    public void onGenericResponse(GenericResponse response) {
        Log.d(TAG, "onGenericResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onOnCommand(OnCommand notification) {
        Log.d(TAG, "onOnCommand() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onAddCommandResponse(AddCommandResponse response) {
        Log.d(TAG, "onAddCommandResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onAddSubMenuResponse(AddSubMenuResponse response) {
        Log.d(TAG, "onAddSubMenuResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onCreateInteractionChoiceSetResponse(CreateInteractionChoiceSetResponse response) {
        Log.d(TAG, "onCreateInteractionChoiceSetResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onAlertResponse(AlertResponse response) {
        Log.d(TAG, "onAlertResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onDeleteCommandResponse(DeleteCommandResponse response) {
        Log.d(TAG, "onDeleteCommandResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onDeleteInteractionChoiceSetResponse(DeleteInteractionChoiceSetResponse response) {
        Log.d(TAG, "onDeleteInteractionChoiceSetResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {
        Log.d(TAG, "onDeleteSubMenuResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onPerformInteractionResponse(PerformInteractionResponse response) {
        Log.d(TAG, "onPerformInteractionResponse() called with: response = [" + serializeOrFart(response) + "]");
        if (response.getChoiceID() == null) return;
        //if the choice ID isn't associated with a FeedItem, it's associated with a Feed and we need to prompt for that
        //feed's feeditems.
        if (feedItemChoiceIDs.get(response.getChoiceID()) == null) {
            PerformInteraction performInteraction = new PerformInteraction();
            performInteraction.setInteractionMode(InteractionMode.MANUAL_ONLY);
            performInteraction.setInteractionLayout(LayoutMode.LIST_ONLY);
            List<Integer> choiceIds = new ArrayList<>();
            choiceIds.add(response.getChoiceID() + FEED_CHOICESET_ID_BASE);
            performInteraction.setInteractionChoiceSetIDList(choiceIds);
            performInteraction.setInitialText(playerFacade.getFeeds().get(response.getChoiceID()).getTitle());
            sendRPCDammit(performInteraction);
        } else {
            DBTasks.playMedia(this, feedItemChoiceIDs.get(response.getChoiceID()).getMedia(), false, true, false);
        }
    }

    @Override
    public void onResetGlobalPropertiesResponse(ResetGlobalPropertiesResponse response) {
        Log.d(TAG, "onResetGlobalPropertiesResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse response) {
        Log.d(TAG, "onSetGlobalPropertiesResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {
        Log.d(TAG, "onSetMediaClockTimerResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onShowResponse(ShowResponse response) {
        Log.d(TAG, "onShowResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onSpeakResponse(SpeakResponse response) {
        Log.d(TAG, "onSpeakResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onOnButtonEvent(OnButtonEvent notification) {
        Log.d(TAG, "onOnButtonEvent() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onOnButtonPress(OnButtonPress notification) {
        Log.d(TAG, "onOnButtonPress() called with: notification = [" + serializeOrFart(notification) + "]");

        switch (notification.getButtonName()) {
            case OK:
                Log.i(TAG, "onOnButtonPress: OK (play/pause) pressed");
                playerFacade.togglePlayPause();
                break;
            case SEEKLEFT:
                Log.i(TAG, "onOnButtonPress: SEEKLEFT pressed");
                playerFacade.jumpBackward();
                break;
            case SEEKRIGHT:
                Log.i(TAG, "onOnButtonPress: SEEKRIGHT pressed");
                playerFacade.jumpForward();
                break;
            case CUSTOM_BUTTON:
                Log.i(TAG, "onOnButtonPress: CUSTOM: " + notification.getCustomButtonName());
                switch (notification.getCustomButtonName()) {
                    case PREV_SOFTBUTTON_ID:
                        Log.i(TAG, "onOnButtonPress: Previous button pressed, maybe going back?");
                        playerFacade.previous();
                        break;
                    case NEXT_SOFTBUTTON_ID:
                        Log.i(TAG, "onOnButtonPress: Next button pressed, going to next episode?");
                        playerFacade.next();
                        break;
                    case LIST_SOFTBUTTON_ID:
                        Log.i(TAG, "onOnButtonPress: List button pressed");
                        PerformInteraction performInteraction = new PerformInteraction();
                        performInteraction.setInitialText("Feeds");
                        List<Integer> choiceSetIDs = new ArrayList<>();
                        choiceSetIDs.add(FEEDS_CHOICESET_ID);
                        performInteraction.setInteractionChoiceSetIDList(choiceSetIDs);
                        performInteraction.setInteractionLayout(LayoutMode.LIST_ONLY);
                        performInteraction.setInteractionMode(InteractionMode.MANUAL_ONLY);
                        sendRPCDammit(performInteraction);
                }
        }
    }

    @Override
    public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
        Log.d(TAG, "onSubscribeButtonResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
        Log.d(TAG, "onUnsubscribeButtonResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onOnPermissionsChange(OnPermissionsChange notification) {
        Log.d(TAG, "onOnPermissionsChange() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onSubscribeVehicleDataResponse(SubscribeVehicleDataResponse response) {
        Log.d(TAG, "onSubscribeVehicleDataResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onUnsubscribeVehicleDataResponse(UnsubscribeVehicleDataResponse response) {
        Log.d(TAG, "onUnsubscribeVehicleDataResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onGetVehicleDataResponse(GetVehicleDataResponse response) {
        Log.d(TAG, "onGetVehicleDataResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onOnVehicleData(OnVehicleData notification) {
        Log.d(TAG, "onOnVehicleData() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onPerformAudioPassThruResponse(PerformAudioPassThruResponse response) {
        Log.d(TAG, "onPerformAudioPassThruResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onEndAudioPassThruResponse(EndAudioPassThruResponse response) {
        Log.d(TAG, "onEndAudioPassThruResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onOnAudioPassThru(OnAudioPassThru notification) {
        Log.d(TAG, "onOnAudioPassThru() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onPutFileResponse(PutFileResponse response) {
        Log.d(TAG, "onPutFileResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onDeleteFileResponse(DeleteFileResponse response) {
        Log.d(TAG, "onDeleteFileResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onListFilesResponse(ListFilesResponse response) {
        Log.d(TAG, "onListFilesResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onSetAppIconResponse(SetAppIconResponse response) {
        Log.d(TAG, "onSetAppIconResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onScrollableMessageResponse(ScrollableMessageResponse response) {
        Log.d(TAG, "onScrollableMessageResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onChangeRegistrationResponse(ChangeRegistrationResponse response) {
        Log.d(TAG, "onChangeRegistrationResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse response) {
        Log.d(TAG, "onSetDisplayLayoutResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onOnLanguageChange(OnLanguageChange notification) {
        Log.d(TAG, "onOnLanguageChange() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onOnHashChange(OnHashChange notification) {
        Log.d(TAG, "onOnHashChange() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onSliderResponse(SliderResponse response) {
        Log.d(TAG, "onSliderResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onOnDriverDistraction(OnDriverDistraction notification) {
        Log.d(TAG, "onOnDriverDistraction() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onOnTBTClientState(OnTBTClientState notification) {
        Log.d(TAG, "onOnTBTClientState() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onOnSystemRequest(OnSystemRequest notification) {
        Log.d(TAG, "onOnSystemRequest() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onSystemRequestResponse(SystemRequestResponse response) {
        Log.d(TAG, "onSystemRequestResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onOnKeyboardInput(OnKeyboardInput notification) {
        Log.d(TAG, "onOnKeyboardInput() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onOnTouchEvent(OnTouchEvent notification) {
        Log.d(TAG, "onOnTouchEvent() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onDiagnosticMessageResponse(DiagnosticMessageResponse response) {
        Log.d(TAG, "onDiagnosticMessageResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onReadDIDResponse(ReadDIDResponse response) {
        Log.d(TAG, "onReadDIDResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onGetDTCsResponse(GetDTCsResponse response) {
        Log.d(TAG, "onGetDTCsResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onOnLockScreenNotification(OnLockScreenStatus notification) {
        Log.d(TAG, "onOnLockScreenNotification() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onDialNumberResponse(DialNumberResponse response) {
        Log.d(TAG, "onDialNumberResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onSendLocationResponse(SendLocationResponse response) {
        Log.d(TAG, "onSendLocationResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        boolean forceConnect = intent !=null && intent.getBooleanExtra(TransportConstants.FORCE_TRANSPORT_CONNECTED, false);
        if (proxy == null) {
            try {
                //Create a new proxy using Bluetooth transport
                //The listener, app name,
                //whether or not it is a media app and the applicationId are supplied.
                proxy = new SdlProxyALM(this.getBaseContext(),this, "AntennaPod", true, "12345678");
            } catch (SdlException e) {
                //There was an error creating the proxy
                if (proxy == null) {
                    //Stop the SdlService
                    stopSelf();
                }
            }
        }else if(forceConnect){
            proxy.forceOnConnected();
        }

        //use START_STICKY because we want the SDLService to be explicitly started and stopped as needed.
        return START_STICKY;
    }

    @Override
    public void onShowConstantTbtResponse(ShowConstantTbtResponse response) {
        Log.d(TAG, "onShowConstantTbtResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onAlertManeuverResponse(AlertManeuverResponse response) {
        Log.d(TAG, "onAlertManeuverResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onUpdateTurnListResponse(UpdateTurnListResponse response) {
        Log.d(TAG, "onUpdateTurnListResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onServiceDataACK(int dataSize) {

    }

    @Override
    public void onGetWayPointsResponse(GetWayPointsResponse response) {
        Log.d(TAG, "onGetWayPointsResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onSubscribeWayPointsResponse(SubscribeWayPointsResponse response) {
        Log.d(TAG, "onSubscribeWayPointsResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onUnsubscribeWayPointsResponse(UnsubscribeWayPointsResponse response) {
        Log.d(TAG, "onUnsubscribeWayPointsResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onOnWayPointChange(OnWayPointChange notification) {
        Log.d(TAG, "onOnWayPointChange() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onGetSystemCapabilityResponse(GetSystemCapabilityResponse response) {
        Log.d(TAG, "onGetSystemCapabilityResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onGetInteriorVehicleDataResponse(GetInteriorVehicleDataResponse response) {
        Log.d(TAG, "onGetInteriorVehicleDataResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onButtonPressResponse(ButtonPressResponse response) {
        Log.d(TAG, "onButtonPressResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onSetInteriorVehicleDataResponse(SetInteriorVehicleDataResponse response) {
        Log.d(TAG, "onSetInteriorVehicleDataResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onOnInteriorVehicleData(OnInteriorVehicleData notification) {
        Log.d(TAG, "onOnInteriorVehicleData() called with: notification = [" + serializeOrFart(notification) + "]");
    }

    @Override
    public void onSendHapticDataResponse(SendHapticDataResponse response) {
        Log.d(TAG, "onSendHapticDataResponse() called with: response = [" + serializeOrFart(response) + "]");
    }

    @Override
    public void onPositionUpdate(int positionMsec, int durationMsec) {
        SetMediaClockTimer mediaClockTimer = new SetMediaClockTimer();
        mediaClockTimer.setStartTime(toStartTime(positionMsec));
        mediaClockTimer.setEndTime(toStartTime(durationMsec));
        mediaClockTimer.setUpdateMode(playerFacade.isPlaying() ? UpdateMode.COUNTUP : UpdateMode.PAUSE);
        Log.d(TAG, "onPositionObserverUpdate: SetMediaClockTimer to be sent: " + serializeOrFart(mediaClockTimer));
        sendRPCDammit(mediaClockTimer);
    }

    @Override
    public void onMediaInfoUpdate(String episodeTitle, String feedTitle) {
        Show show = new Show();
        show.setMainField1(episodeTitle);
        show.setMainField2(feedTitle);
        sendRPCDammit(show);
    }

    @Override
    public void onPlaybackEnd() {
        Show show = new Show();
        show.setMainField1("");
        show.setMainField2("");
        sendRPCDammit(show);
    }
}
