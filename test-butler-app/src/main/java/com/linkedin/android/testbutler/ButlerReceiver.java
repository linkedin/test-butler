package com.linkedin.android.testbutler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ButlerReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		context.startService(new Intent(context, ButlerService.class).putExtras(intent.getExtras()));
	}
}
