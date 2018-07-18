package de.danoeh.antennapod.sdl;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.smartdevicelink.transport.SdlBroadcastReceiver;
import com.smartdevicelink.transport.SdlRouterService;

public class SdlReceiver extends SdlBroadcastReceiver {
    private static final String TAG = "SdlReceiver";

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
}
