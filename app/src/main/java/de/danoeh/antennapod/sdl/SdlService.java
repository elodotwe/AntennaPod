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

import com.smartdevicelink.exception.SdlException;
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
import com.smartdevicelink.proxy.rpc.PerformInteractionResponse;
import com.smartdevicelink.proxy.rpc.PutFileResponse;
import com.smartdevicelink.proxy.rpc.ReadDIDResponse;
import com.smartdevicelink.proxy.rpc.ResetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.ScrollableMessageResponse;
import com.smartdevicelink.proxy.rpc.SendHapticDataResponse;
import com.smartdevicelink.proxy.rpc.SendLocationResponse;
import com.smartdevicelink.proxy.rpc.SetAppIconResponse;
import com.smartdevicelink.proxy.rpc.SetDisplayLayoutResponse;
import com.smartdevicelink.proxy.rpc.SetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.SetInteriorVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.SetMediaClockTimerResponse;
import com.smartdevicelink.proxy.rpc.ShowConstantTbtResponse;
import com.smartdevicelink.proxy.rpc.ShowResponse;
import com.smartdevicelink.proxy.rpc.SliderResponse;
import com.smartdevicelink.proxy.rpc.SpeakResponse;
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
import com.smartdevicelink.proxy.rpc.enums.ButtonName;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.SdlDisconnectedReason;
import com.smartdevicelink.transport.TransportConstants;

import org.json.JSONException;

public class SdlService extends Service implements IProxyListenerALM {
    private final static int NOTIFICATION_ID = 1; // Chosen randomly by rolling a 6 sided die
    private final static String NOTIF_CHANNEL_ID = "AntennaPod";
    private final static String TAG = "SdlService";

    //The proxy handles communication between the application and SDL
    private SdlProxyALM proxy = null;

    private boolean isFirstRun = true;

    private String serializeOrFart(RPCStruct struct) {
        try {
            return struct.serializeJSON().toString(2);
        } catch (JSONException e) {
            return "Could not serialize struct.";
        }
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

        super.onDestroy();
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) return;

            NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID, "AntennaPod", NotificationManager.IMPORTANCE_NONE);
            notificationManager.createNotificationChannel(channel);
            Notification serviceNotification = new Notification.Builder(this, "AntennaPod")
            .setContentTitle("AntennaPod SDL")
            .setContentText("AntenaPod is mayhaps connecting to your car.")
                    .setChannelId(NOTIF_CHANNEL_ID)
                    .build();
            startForeground(NOTIFICATION_ID, serviceNotification);
        }
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
            Log.e(TAG, "onOnHMIStatus: Unable to subscribe to button " + buttonName.name(), e);
        }
    }

    @Override
    public void onOnHMIStatus(OnHMIStatus notification) {
        Log.d(TAG, "onOnHMIStatus() called with: notification = [" + serializeOrFart(notification) + "]");

        if (notification.getHmiLevel() == HMILevel.HMI_FULL) {
            if (isFirstRun) {
                subscribeButton(ButtonName.OK);
                subscribeButton(ButtonName.SEEKLEFT);
                subscribeButton(ButtonName.SEEKRIGHT);

                isFirstRun = false;
            }
        }
    }

    @Override
    public void onProxyClosed(String info, Exception e, SdlDisconnectedReason reason) {
        Log.e(TAG, "onProxyClosed; spinning down service. info = " + info + "; reason = " + reason.name(), e);
        stopSelf();
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
}
