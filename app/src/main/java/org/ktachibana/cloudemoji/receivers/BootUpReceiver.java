package org.ktachibana.cloudemoji.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.ktachibana.cloudemoji.activities.BootUpDummyActivity;

/**
 * Shows notification when booted up
 */
public class BootUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent dummyActivityIntent = new Intent(context, BootUpDummyActivity.class);
            dummyActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            dummyActivityIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            context.startActivity(dummyActivityIntent);
        }
    }
}
