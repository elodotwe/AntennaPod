package de.danoeh.antennapod.sdl;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.smartdevicelink.transport.SdlBroadcastReceiver;
import com.smartdevicelink.transport.SdlRouterService;
import com.smartdevicelink.transport.TransportConstants;

public class SdlReceiver extends SdlBroadcastReceiver {
    private static final String TAG = "SdlReceiver";
    public static final String RECONNECT_LANG_CHANGE = "RECONNECT_LANG_CHANGE";

    @Override
    public Class<? extends SdlRouterService> defineLocalSdlRouterClass() {
        return de.danoeh.antennapod.sdl.SdlRouterService.class;
    }

    @Override
    public void onSdlEnabled(Context context, Intent intent) {
        Log.i(TAG, "onSdlEnabled; spinning up service");
        //Use the provided intent but set the class to the SdlService
        intent.setClass(context, SdlService.class);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            context.startService(intent);
        }else{
            context.startForegroundService(intent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive() called with: context = [" + context + "], intent = [" + intent + "]");
        super.onReceive(context, intent); // Required if overriding this method

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                Log.d(TAG, String.format("%s %s (%s)", key,
                        value.toString(), value.getClass().getName()));
            }
        }

        if (intent != null) {
            String action = intent.getAction();
            if (action != null){
                Log.i(TAG, "onReceive: action is " + action);
                if(action.equalsIgnoreCase(TransportConstants.START_ROUTER_SERVICE_ACTION)) {

                    if (intent.getBooleanExtra(RECONNECT_LANG_CHANGE, false)) {
                        Log.i(TAG, "onReceive: running onSdlEnabled");
                        onSdlEnabled(context, intent);
                    }
                }
            }
        }
    }
}
