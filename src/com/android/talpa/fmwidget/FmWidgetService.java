package com.android.talpa.fmwidget;

import java.text.NumberFormat;
import java.util.Locale;

import com.android.talpa.fmwidget.R;
import com.talpa.manager.ITalpaFmClientCallback;
import com.talpa.manager.ITalpaFmService;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * @author junjie.shi
 */
public class FmWidgetService extends Service {

	public static final int PRE_STATION = 11;
	public static final int NEXT_STATION = 12;

	public static final int DECREASE_STATION = 13;
	public static final int INCREASE_STATION = 14;

	public static final int PLAY = 15;

	public static final int TO_FM_APP = 111;

	public static final String ACTION = "fm_action";

	public static final int ACTION_ID = 1;
	public static final int DEFAULT_ACTION_ID = 0;

	private static final String STATION = "station";
	private static final String STATION_KEY = "station_key";
	private SharedPreferences preferences;

	private ITalpaFmService mRemoteService;
	private boolean isUpdate = false;
	private boolean isConn = false;
	private boolean isPlay = false;

	private float mCurrentStation = 87.5f;
	private float mOldStation = 0;

	private AppWidgetManager mAppWidgetManager;
	private boolean isDeferPlay = false;

	private int[] mDigit = { R.drawable.fm_0, R.drawable.fm_1, R.drawable.fm_2, R.drawable.fm_3, R.drawable.fm_4,
			R.drawable.fm_5, R.drawable.fm_6, R.drawable.fm_7, R.drawable.fm_8, R.drawable.fm_9 };

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			runFloat();
		};
	};

	@Override
	public void onCreate() {
		super.onCreate();
		preferences = getSharedPreferences(STATION, Context.MODE_PRIVATE);
		mCurrentStation = preferences.getFloat(STATION_KEY, mCurrentStation);

		registerFmRadioDeviceReceiver();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// if (!isConn || mRemoteService == null) {
		// conn();
		// }

		if (intent == null) {
			return 1;
		}

		int extra = intent.getIntExtra(ACTION, DEFAULT_ACTION_ID);
		handEventFm(extra);

		Log.v("lmjssjj", "updatewidget");
		updateWidget();

		return Service.START_STICKY;
	}

	private void refreshStationUI(float f) {

		mAppWidgetManager = AppWidgetManager.getInstance(this);

		ComponentName provider = new ComponentName(this, FmWidgetProvider.class);
		int[] widgetIds = mAppWidgetManager.getAppWidgetIds(provider);

		for (int i : widgetIds) {

			RemoteViews remoteView = new RemoteViews(getPackageName(), R.layout.fm_widget);

			if (isRtl(getResources())) {

				remoteView.setImageViewResource(R.id.v_digit_4_rtl, mDigit[(int) (f / 100) % 10]);
				remoteView.setImageViewResource(R.id.v_digit_3_rtl, mDigit[(int) (f / 10) % 10]);
				remoteView.setImageViewResource(R.id.v_digit_2_rtl, mDigit[(int) (f) % 10]);
				remoteView.setImageViewResource(R.id.v_digit_1_rtl, mDigit[(int) (f * 10) % 10]);

			} else {

				remoteView.setImageViewResource(R.id.v_digit_1, mDigit[(int) (f / 100) % 10]);
				remoteView.setImageViewResource(R.id.v_digit_2, mDigit[(int) (f / 10) % 10]);
				remoteView.setImageViewResource(R.id.v_digit_3, mDigit[(int) (f) % 10]);
				remoteView.setImageViewResource(R.id.v_digit_4, mDigit[(int) (f * 10) % 10]);
			}
			// 更新 widget
			mAppWidgetManager.updateAppWidget(i, remoteView);
			// Log.v("lmjssjj", "refreshStationUI->widgetId"+i+" station:"+f);
		}

	}

	/**
	 * 跑小数动画
	 */
	private void runFloat() {
		Log.v("lmjssjj", "runFloat->mOldStation:" + mOldStation);
		Log.v("lmjssjj", "runFloat->mCurrentStation:" + mCurrentStation);

		final NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(1);
		nf.setMinimumIntegerDigits(3);

		ValueAnimator valueAnimator = ValueAnimator.ofFloat(mOldStation, mCurrentStation);
		valueAnimator.setDuration(500);
		valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {

				float value = (Float) valueAnimator.getAnimatedValue();
				// Log.v("lmjssjj", "runFloat:" + value);
				refreshStationUI(Float.valueOf(nf.format(value)));

			}

		});

		valueAnimator.start();
	}

	private void refreshButtonUI() {

		mAppWidgetManager = AppWidgetManager.getInstance(this);

		ComponentName provider = new ComponentName(this, FmWidgetProvider.class);
		int[] widgetIds = mAppWidgetManager.getAppWidgetIds(provider);

		for (int i : widgetIds) {

			RemoteViews remoteView = new RemoteViews(getPackageName(), R.layout.fm_widget);

			remoteView.setImageViewResource(R.id.btn_open,
					isPlay ? R.drawable.fm_pause_selector : R.drawable.fm_open_selector);

			// 更新 widget
			mAppWidgetManager.updateAppWidget(i, remoteView);

			Log.v("lmjssjj", "refreshButtonUI->widgetId" + i);

		}

	}

	private void updateWidget() {

		Log.v("lmjssjj", "updateWidget->isplay:" + isPlay);
		Log.v("lmjssjj", "updateWidget->isConn:" + isConn);
		Log.v("lmjssjj", "updateWidget->station:" + mCurrentStation);

		mAppWidgetManager = AppWidgetManager.getInstance(this);

		ComponentName provider = new ComponentName(this, FmWidgetProvider.class);
		int[] widgetIds = mAppWidgetManager.getAppWidgetIds(provider);

		for (int i : widgetIds) {

			RemoteViews remoteView = new RemoteViews(getPackageName(), R.layout.fm_widget);

			// bug 19255
			if (isRtl(getResources())) {

				remoteView.setViewVisibility(R.id.digit_layout, View.INVISIBLE);
				remoteView.setViewVisibility(R.id.digit_layout_rtl, View.VISIBLE);

				// rtl button
				remoteView.setImageViewResource(R.id.btn_next, R.drawable.fm_pre_selector);
				remoteView.setImageViewResource(R.id.btn_pre, R.drawable.fm_next_selector);
				remoteView.setOnClickPendingIntent(R.id.btn_pre, getPendingIntent(NEXT_STATION, i));
				remoteView.setOnClickPendingIntent(R.id.btn_next, getPendingIntent(PRE_STATION, i));

				// rtl digit
				remoteView.setImageViewResource(R.id.v_digit_4_rtl, mDigit[(int) (mCurrentStation / 100) % 10]);
				remoteView.setImageViewResource(R.id.v_digit_3_rtl, mDigit[(int) (mCurrentStation / 10) % 10]);
				remoteView.setImageViewResource(R.id.v_digit_2_rtl, mDigit[(int) (mCurrentStation) % 10]);
				remoteView.setImageViewResource(R.id.v_digit_1_rtl, mDigit[(int) (mCurrentStation * 10) % 10]);

			} else {
				// rtl
				remoteView.setViewVisibility(R.id.digit_layout, View.VISIBLE);
				remoteView.setViewVisibility(R.id.digit_layout_rtl, View.INVISIBLE);

				// select station event
				remoteView.setOnClickPendingIntent(R.id.btn_next, getPendingIntent(NEXT_STATION, i));
				remoteView.setOnClickPendingIntent(R.id.btn_pre, getPendingIntent(PRE_STATION, i));

				// digit view
				remoteView.setImageViewResource(R.id.v_digit_1, mDigit[(int) (mCurrentStation / 100) % 10]);
				remoteView.setImageViewResource(R.id.v_digit_2, mDigit[(int) (mCurrentStation / 10) % 10]);
				remoteView.setImageViewResource(R.id.v_digit_3, mDigit[(int) (mCurrentStation) % 10]);
				remoteView.setImageViewResource(R.id.v_digit_4, mDigit[(int) (mCurrentStation * 10) % 10]);

			}

			// bg event
			remoteView.setOnClickPendingIntent(R.id.root_view, goToMainFm());
			// play event
			remoteView.setOnClickPendingIntent(R.id.btn_open, getPendingIntent(PLAY, i));
			remoteView.setImageViewResource(R.id.btn_open,
					isPlay ? R.drawable.fm_pause_selector : R.drawable.fm_open_selector);

			Log.v("lmjssjj", "updateAppWidget  widgetId:" + i);

			// 更新 widget
			mAppWidgetManager.updateAppWidget(i, remoteView);

		}

	}

	private void handEventFm(int actionid) {

		Log.v("lmjssjj", "handevent actionid" + actionid);
		switch (actionid) {
		case PRE_STATION:

			preStation();

			break;
		case NEXT_STATION:

			nextStation();

			break;
		case DECREASE_STATION:

			de();

			break;
		case INCREASE_STATION:

			in();

			break;
		case PLAY:

			play();

			break;

		default:
			break;
		}
	}

	private PendingIntent getPendingIntent(int actionid, int widgetId) {
		Intent intent = new Intent();
		intent.setClass(this, FmWidgetService.class);
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		intent.putExtra(ACTION, actionid);
		PendingIntent pi = PendingIntent.getService(this, widgetId * actionid, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		Log.v("lmjssjj", "getPendingIntent:" + widgetId * actionid + "  " + pi.toString());

		return pi;
	}

	private Intent getRemoteIntent() {
		Intent service = new Intent();
		service.addCategory(Intent.CATEGORY_DEFAULT);
		ComponentName componentName = new ComponentName("com.android.fmradio", "com.talpa.manager.TalpaFmService");
		service.setComponent(componentName);
		return service;
	}

	public void conn() {
		if (isConn) {
			return;
		}
		Log.v("lmjssjj", "conn");

		boolean b = bindService(getRemoteIntent(), mServiceConnection, Context.BIND_AUTO_CREATE);

		Log.v("lmjssjj", "isbindservice:" + b);
	}

	private void reBindService() {
		Log.v("lmjssjj", "reBindService");
		boolean b = bindService(getRemoteIntent(), mServiceConnection, Context.BIND_AUTO_CREATE);
		Log.v("lmjssjj", "reBindService->isbindservice:" + b);
	}

	public void play() {
		Log.v("lmjssjj", "play");
		if (mRemoteService == null) {
			Log.v("lmjssjj", "mRemoteService:null-->isDeferPlay = true");
			isDeferPlay = true;
			reBindService();
			return;
		}
		try {
			mRemoteService.play();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void nextStation() {
		Log.v("lmjssjj", "next");
		if (isUpdate || !isPlay) {
			return;
		}
		if (mRemoteService == null) {
			Log.v("lmjssjj", "mRemoteService:null");
			reBindService();
			return;
		}
		isUpdate = true;
		try {
			mRemoteService.seekStation(0, true);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void preStation() {
		Log.v("lmjssjj", "pre");
		if (isUpdate || !isPlay) {
			return;
		}
		if (mRemoteService == null) {
			Log.v("lmjssjj", "mRemoteService:null");
			reBindService();
			return;
		}
		isUpdate = true;
		try {
			mRemoteService.seekStation(0, false);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void in() {
		Log.v("lmjssjj", "in");
		if (isUpdate || !isPlay) {
			return;
		}
		if (mRemoteService == null) {
			Log.v("lmjssjj", "mRemoteService:null");
			reBindService();
			return;
		}
		isUpdate = true;
		try {
			mRemoteService.tuneIncreaseStation(0);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void de() {
		Log.v("lmjssjj", "de");
		if (isUpdate || !isPlay) {
			return;
		}
		if (mRemoteService == null) {
			Log.v("lmjssjj", "mRemoteService:null");
			reBindService();
			return;
		}
		isUpdate = true;
		try {
			mRemoteService.tuneDecreaseStation(0);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.v("lmjssjj", "widget client service conn remote succce");
			isConn = true;
			mRemoteService = ITalpaFmService.Stub.asInterface(service);
			try {
				if (isDeferPlay) {
					isDeferPlay = false;
					mRemoteService.play();
					Log.v("lmjssjj", "widget isDeferPlay-->mRemoteService.play()");
				}
				mRemoteService.registerCallback(mCallback);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			try {
				mRemoteService.unregisterCallback(mCallback);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			mRemoteService = null;
			isConn = false;
			Log.v("lmjssjj", "client->onServiceDisconnected");
		}
	};

	private ITalpaFmClientCallback mCallback = new ITalpaFmClientCallback.Stub() {

		@Override
		public void updateStation(final String station) throws RemoteException {
			Log.v("lmjssjj", "client->updateStation:" + station);
			isUpdate = false;
			// mCurrentStation = Float. (station);
			mOldStation = mCurrentStation;

			mCurrentStation = Float.valueOf(station);

			mHandler.sendEmptyMessage(0);

			saveStation(mCurrentStation);
		}

		@Override
		public void updateAntenna(boolean b) throws RemoteException {
			Log.v("lmjssjj", "client->updateAntenna:" + b);
		}

		@Override
		public void updateIsPlay(boolean isPlay) throws RemoteException {
			Log.v("lmjssjj", "client->updateIsPlay:" + isPlay);
			FmWidgetService.this.isPlay = isPlay;
			// refreshButtonUI();
			updateWidget();
		}

		@Override
		public void updateTurnFinished(String station) throws RemoteException {
			Log.v("lmjssjj", "client->updateTurnFinished:" + station);
			// mOldStation = mCurrentStation;
			//
			// mCurrentStation = Float.valueOf(station);
			//
			// mHandler.sendEmptyMessage(0);

		}

		@Override
		public void initPlayState(boolean isPlay, String station) throws RemoteException {
			FmWidgetService.this.isPlay = isPlay;
			mCurrentStation = Float.valueOf(station);
			// updateWidget();
			Log.v("lmjssjj", "client-------------->initPlayState:" + isPlay);
			Log.v("lmjssjj", "client------------>initPlayState:" + mCurrentStation);
		}
	};

	@Override
	public void onDestroy() {
		try {
			if (isPlay) {
				play();
			}
			unbindService(mServiceConnection);
			unRegisterFmRadioDeviceReceiver();
			Log.v("lmjssjj", "onDestroy");
			super.onDestroy();
		} catch (Exception e) {
			Log.e("lmjssjj", e.toString());
		}
	}

	private PendingIntent goToMainFm() {
		Intent intent = new Intent();
		ComponentName component = new ComponentName("com.android.fmradio", "com.android.fmradio.FmMainActivity");
		intent.setComponent(component);

		PendingIntent toFm = PendingIntent.getActivity(this, TO_FM_APP, intent, 0);

		return toFm;
	}

	private void saveStation(float station) {
		if (preferences == null) {
			preferences = getSharedPreferences(STATION, Context.MODE_PRIVATE);
		}
		Editor edit = preferences.edit();
		edit.putFloat(STATION_KEY, station);
		edit.commit();
	}

	public static final boolean ATLEAST_JB_MR1 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public static boolean isRtl(Resources res) {
		return ATLEAST_JB_MR1 && (res.getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
	}

	private FmStateReceiver mReceiver;
	private static final String ACTION_FM_POWERDOWN = "android.talpa.action.POWER_DOWN";
	private static final String ACTION_FM_DEVICE_STATE = "android.talpa.action.FMDEVICE_STATS";
	private static final String ACTION_FM_DEVICE_STATE_KEY = "state_key";

	private class FmStateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.v("lmjssjj", "widget onreceive:"+action);
			if (ACTION_FM_DEVICE_STATE.equals(action)) {

				Bundle extras = intent.getExtras();
				boolean b = extras.getBoolean(ACTION_FM_DEVICE_STATE_KEY, true);
				Log.v("lmjssjj", "fmwidget ----onReceive->stats----" + b);
				Log.v("lmjssjj", "fmwidget ----onReceive->mRemoteService----" + mRemoteService);
				if (b && mRemoteService == null) {
					reBindService();
				} else {
					// unbindService(mServiceConnection);
				}
			}else if(ACTION_FM_POWERDOWN.equals(action)){
				FmWidgetService.this.isPlay = false;
				updateWidget();
			}
		}

	}

	private void registerFmRadioDeviceReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_FM_DEVICE_STATE);
		filter.addAction(ACTION_FM_POWERDOWN);

		mReceiver = new FmStateReceiver();
		registerReceiver(mReceiver, filter);
	}

	private void unRegisterFmRadioDeviceReceiver() {
		unregisterReceiver(mReceiver);
	}
}
