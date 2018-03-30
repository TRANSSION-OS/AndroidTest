package com.android.talpa.fmwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
/**
 * @author junjie.shi
 */
public class FmWidgetProvider extends AppWidgetProvider {

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		Log.v("lmjssjj", "fm widget onReceive:"+intent.getAction());
		Intent service = new Intent(context, FmWidgetService.class);
		service.putExtra("update", true);
		
		context.startService(service );
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		Log.v("lmjssjj", "fm widget onUpdate");
		Intent service = new Intent(context, FmWidgetService.class);
		service.putExtra("update", true);
		
		context.startService(service );
		
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		Log.v("lmjssjj", "fm widget onDeleted");
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		Log.v("lmjssjj", "fm widget onEnabled");
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		Log.v("lmjssjj", "fm widget onDisabled");
		context.stopService(new Intent(context, FmWidgetService.class));
		
	}

}
