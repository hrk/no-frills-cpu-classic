package it.sineo.android.noFrillsCPUClassic;

import it.sineo.android.noFrillsCPUClassic.service.BootService;
import it.sineo.android.noFrillsCPUClassic.service.ShutdownService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Intent _intent = new Intent(context, BootService.class);
			context.startService(_intent);
		} else if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
			Intent _intent = new Intent(context, ShutdownService.class);
			context.startService(_intent);
		}
	}
}
