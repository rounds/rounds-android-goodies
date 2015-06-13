/*
 * FlexibleIntentService is derived from the 
 * source code of the android IntentService,
 * which is part of the Android Open Source Project. 
 * 
 * The original IntentService source code is open source
 * code under this license:
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * --------------------------------------------------------
 */

package com.rounds.goodies.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

/***
 * <p>
 * A base service class, similar to {@link android.app.IntentService} but more
 * flexible allowing you to delay or retry actions when you handle them. <br>
 * In addition if you want you can keep the service alive for as long as you
 * want by changing the value returned by {@link #getStopMode()}.
 * </p>
 * 
 * <h1>How to use FlexibleIntentService</h1>
 * <ul>
 * <li>
 * Create your own service subclass that extends FlexibleIntentService</li>
 * <li>
 * Your subclass should implement the abstract method <br>
 * {@link #onHandleIntent(Intent intent,int startid)} <br>
 * and handle intents according to the intent action and bundle of extra params <br>
 * If you want to delay this action for later or retry an action that failed
 * call one of the below two methods from onHandleIntent <br>
 * {@link #sendDelayedAction(String, int, long, int)} or <br>
 * {@link #sendDelayedAction(String, Bundle, int, long, int)}. <br>
 * To check if the service has delayed actions that have not yet been run:<br>
 * {@link #hasDelayedActionsWithType(int)}. <br>
 * If you need to cancel all delayed actions of a certain type call:<br>
 * {@link #cancelDelayedActionsWithType(int)}</li>
 * <li>
 * Your subclass should also implement {@link #getDelayedActionTypes()}<br>
 * Calling sendDelayedAction with a delayedActionType that is not in this list
 * will throw an IllegalArgumentException</li>
 * <li>
 * If you decide to use {@link #MANUAL_STOP_MODE},<br>
 * you can stop the service in one of the following ways:<br>
 * 1. Call context.stopService(intent) from an external component<br>
 * 2. Call {@link #sendManualStopServiceIntent()} and then if needed override
 * {@link #onHandleManualStopService(int, int)}
 * </ul>
 * </p>
 * 
 */
public abstract class FlexibleIntentService extends Service {
	/**
	 * This is the default mode for this service The service will stop itself
	 * when all intents sent to it including delayed actions have finished and
	 * there is nothing more left in it's queue.
	 */
	public static final byte AUTO_STOP_MODE = 0;

	/**
	 * In this mode you MUST call stopSelf() or stopService to stop the service
	 * and take care of waiting or canceling all delayed actions yourself
	 */
	public static final byte MANUAL_STOP_MODE = 1;

	// internal action, used by sendStopServiceIntent() method
	private static final String STOP_SERVICE_ACTION = "FlexibleIntentService.STOP_SERVICE_ACTION";

	private volatile Looper mServiceLooper;
	private volatile ServiceHandler mServiceHandler;
	private String mName;

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper){
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			Intent intent = (Intent) msg.obj;
			int stopMode = getStopMode();

			if (stopMode == MANUAL_STOP_MODE && STOP_SERVICE_ACTION.equals(intent.getAction())) {
				onHandleManualStopService(msg.arg1, msg.arg2);
			}
			else {
				onHandleIntent(intent, msg.arg1);

				if (stopMode == AUTO_STOP_MODE && shouldStopService(msg.arg2)) {

					stopSelf(msg.arg1);

				}
			}
		}

		private boolean shouldStopService(int currentActionType) {
			boolean shouldStopService = true;
			// if we have message
			// with current type it is safe to stop
			for (int delayedActionType : getDelayedActionTypes()) {
				if (delayedActionType != currentActionType
						&& hasDelayedActionsWithType(delayedActionType)) {
					// we have a message waiting can't stop service yet
					shouldStopService = false;
				}
			}
			return shouldStopService;
		}
	}

	/**
	 * If you are using {@link #MANUAL_STOP_MODE} it is recommended that you
	 * call this method to stop the service so that you handle the request to
	 * stop the service with a fresh startId. If needed override the
	 * onHandleManualStopService() method to do anything you need to do to stop
	 * the service
	 * @param context a context
	 * @param flexibleIntentServiceClass your subclass
	 */
	protected void sendManualStopServiceIntent() {
		Intent stopServiceIntent = new Intent(this, getClass());
		stopServiceIntent.setAction(FlexibleIntentService.STOP_SERVICE_ACTION);
		startService(stopServiceIntent);
	}

	/**
	 * If you are using {@link #MANUAL_STOP_MODE} take a look at this method, it
	 * will be called when you ask to {@link #sendManualStopServiceIntent()} By
	 * default this method will cancel all delayed actions and stop itself. You
	 * can override it if you want it to behave in a different way
	 * @param startId the startId to use when calling stopSelf
	 * @param currentActionType if this was a delayed message, then this will be
	 *        it's type
	 */
	protected void onHandleManualStopService(int startId, int currentActionType) {
		for (int delayedActionType : getDelayedActionTypes()) {
			if (delayedActionType != currentActionType
					&& hasDelayedActionsWithType(delayedActionType)) {
				// we have a message waiting can't stop service yet
				cancelDelayedActionsWithType(delayedActionType);
			}
		}
		stopSelf(startId);
	}

	/**
	 * Creates a FlexibleIntentService. Invoked by your subclass's constructor.
	 * 
	 * @param name Used to name the worker thread, important only for debugging.
	 */
	public FlexibleIntentService(String name){
		super();
		mName = name;
	}

	/**
	 * This method is invoked on the worker thread with a request to process.
	 * Only one Intent is processed at a time, but the processing happens on a
	 * worker thread that runs independently from other application logic. So,
	 * if this code takes a long time, it will hold up other requests to the
	 * same FlexibleIntentService, but it will not hold up anything else.
	 * 
	 * @param intent The value passed to
	 *        {@link android.content.Context#startService(Intent)}.
	 * @param startId Passed from
	 *        {@link android.content.Context#startService(Intent)}. You may use
	 *        the startId, if you want to call the @link #
	 */
	protected abstract void onHandleIntent(Intent intent, int startId);

	/**
	 * Return a list of all possible delayedActionTypes you may send to
	 * {@link #sendDelayedAction}
	 * @return int[] a list of delayedActionTypes as defined by your service Can
	 *         be null, if you never ask for delayed actions
	 * @see {@link #sendDelayedAction(String, Bundle, int, long, int)}
	 */
	protected abstract int[] getDelayedActionTypes();

	/**
	 * By default the service will stop itself automatically when there are no
	 * more actions to be performed (will also wait for delayed actions). If you
	 * would like to take charge of when to stop this service then override this
	 * method and make it return {@link #MANUAL_STOP_MODE}.<br>
	 * When in {@link #MANUAL_STOP_MODE} the service will never stop itself
	 * Therefore you MUST stop it yourself when the time is right.
	 * @return byte the stop mode can be {@link #AUTO_STOP_MODE} or
	 *         {@link #MANUAL_STOP_MODE}
	 */
	protected byte getStopMode() {
		return AUTO_STOP_MODE;
	}

	/**
	 * Defines what code to return in {@link #onStartCommand(Intent, int, int)}
	 * By default FlexibleIntentService will return
	 * {@link Service#START_REDELIVER_INTENT}
	 * @return int the return code to return in
	 *         {@link #onStartCommand(Intent, int, int)}
	 */
	protected int getStartCommandReturnCode() {
		return START_REDELIVER_INTENT;
	}

	/**
	 * While handling an intent, you can use this method to delay an action to
	 * be handled later. An example of using this would be to retry an action
	 * that failed later
	 * @param delayedAction The string identifying the delayed action
	 * @param extras A Bundle of extra parameters you need to handle the action
	 * @param delayedActionType A type that you define, and that can be used for
	 *        canceling
	 * @param delayMills The number of milliseconds to wait before handling this
	 *        action
	 * @param startId The startId that was passed to the
	 *        {@link #onHandleIntent(Intent, int)}
	 */
	protected final void sendDelayedAction(String delayedAction, Bundle extras,
			int delayedActionType, long delayMills, int startId) {

		int[] allowedDelayedActionTypes = getDelayedActionTypes();
		if (allowedDelayedActionTypes == null) {
			throw new IllegalArgumentException("You need to add delayedActionType "
					+ delayedActionType + ", to the list returned by getDelayedActionTypes() ");
		}

		boolean isActionTypeInList = false;
		for (int actionType : allowedDelayedActionTypes) {
			isActionTypeInList = (actionType == delayedActionType);
			if (isActionTypeInList) {
				break;
			}
		}

		if (!isActionTypeInList) {
			throw new IllegalArgumentException("You need to add delayedActionType "
					+ delayedActionType + ", to the list returned by getDelayedActionTypes() ");
		}
		Message msg = mServiceHandler.obtainMessage();

		Intent handleLaterIntent = new Intent();
		handleLaterIntent.setAction(delayedAction);

		if (extras != null) {
			handleLaterIntent.putExtras(extras);
		}

		msg.obj = handleLaterIntent;
		msg.arg1 = startId;
		msg.what = delayedActionType;
		mServiceHandler.sendMessageDelayed(msg, delayMills);
	}

	/**
	 * While handling an intent, you can use this method to delay an action to
	 * be handled later. An example of using this would be to retry an action
	 * that failed later
	 * @param delayedAction The string identifying the delayed action
	 * @param delayedActionType A type that you define, and that can be used for
	 *        canceling
	 * @param delayMills The number of milliseconds to wait before handling this
	 *        action
	 * @param startId The startId that was passed to the
	 *        {@link #onHandleIntent(Intent, int)}
	 */
	protected final void sendDelayedAction(String delayedAction, int delayedActionType,
			long delayMills, int startId) {
		sendDelayedAction(delayedAction, null, delayedActionType, delayMills, startId);
	}

	/**
	 * Check if there are delayed actions of a given type that are waiting to be
	 * handled
	 * @param delayedActionType the type of delayed actions to check
	 * @return boolean true if there are delayed actions of this type that are
	 *         still waiting to be handled
	 */
	protected boolean hasDelayedActionsWithType(int delayedActionType) {
		return mServiceHandler.hasMessages(delayedActionType);
	}

	/**
	 * Cancel all delayed actions of the specified delayedActionType
	 * @param delayedActionType the type of delayed actions to cancel
	 * @return
	 */
	protected void cancelDelayedActionsWithType(int delayedActionType) {
		mServiceHandler.removeMessages(delayedActionType);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		HandlerThread thread = new HandlerThread("FlexibleIntentService[" + mName + "]");
		thread.start();

		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		msg.obj = intent;
		mServiceHandler.sendMessage(msg);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		onStart(intent, startId);
		return getStartCommandReturnCode();
	}

	@SuppressLint("NewApi")
	@Override
	public void onDestroy() {
		super.onDestroy();
		boolean quitSafelySucceeded = false;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

			try {
				mServiceLooper.quitSafely();
				quitSafelySucceeded = true;

			}
			catch (NoSuchMethodError e) {

			}
		}

		if (quitSafelySucceeded == false) {

			mServiceLooper.quit();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
