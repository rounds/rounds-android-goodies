package com.rounds.goodies.eventpipe;

import java.util.ArrayList;
import java.util.LinkedList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArrayMap;

/**
 * Will keep track, register and unregister BroadcastReceivers for events that a
 * component implementing RoundsBroadcastListener is interested in <br>
 */
public class RoundsEventCommonHandler extends ContextWrapper {

	private LinkedList<BroadcastReceiver> mEventReceivers = new LinkedList<BroadcastReceiver>();
	private ArrayList<IntentFilter> mEventFilters = new ArrayList<IntentFilter>();
	private ArrayMap<String, String> mAddedEvents;
	private RoundsBroadcastListener mBroadcastListener;

	/**
	 * Create new handler that Will keep track, register and unregister BroadcastReceivers for events
	 * that a component implementing RoundsBroadcastListener is interested in <br>
	 * @param context
	 * @param broadcastListener the listener that will handle the events
	 */
	public RoundsEventCommonHandler(Context context, RoundsBroadcastListener broadcastListener){
		super(context.getApplicationContext());
		mBroadcastListener = broadcastListener;
		String[] events = mBroadcastListener.getRoundsEventInterests();
		addNewEvents(events);
	}

	/**
	 * unregister all receivers registered via this handler
	 */
	public void unregisterRoundsEventReceivers() {
		for (BroadcastReceiver receiver : mEventReceivers) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
		}
	}

	/**
	 * register all receivers registered via this handler
	 */	
	public void registerRoundsEventReceivers() {
		int i = 0;
		for (BroadcastReceiver receiver : mEventReceivers) {
			LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
					mEventFilters.get(i++));
		}
	}

	/**
	 * Add a receiver that will handle a single event, and register it
	 * You will be able to unregister this new event too (together with all
	 * others, using the unregisterRoundsEventReceivers() method)
	 * @param event
	 */
	public void addAndRegisterEvent(String event) {

		if (event != null && !mAddedEvents.containsKey(event)) {

			IntentFilter eventFilter = new IntentFilter();

			mAddedEvents.put(event, "");
			eventFilter.addAction(event);

			BroadcastReceiver receiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
					mBroadcastListener.handleRoundsEvent(action, intent.getExtras());
				}
			};

			mEventReceivers.add(receiver);
			mEventFilters.add(eventFilter);

			registerReceiver(receiver, eventFilter);
		}

	}

	private void addNewEvents(String[] events) {

		if (events != null && events.length > 0) {
			if (mAddedEvents == null) {
				mAddedEvents = new ArrayMap<String, String>();
			}

			IntentFilter eventFilter = new IntentFilter();

			for (String event : events) {
				if (!mAddedEvents.containsKey(event)) {
					mAddedEvents.put(event, "");
					eventFilter.addAction(event);
				}
			}

			if (eventFilter.countActions() > 0) {

				BroadcastReceiver receiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						String action = intent.getAction();
						mBroadcastListener.handleRoundsEvent(action, intent.getExtras());
					}
				};

				mEventReceivers.add(receiver);
				mEventFilters.add(eventFilter);
			}
		}
	}
}
