package com.rounds.goodies;

import android.app.Activity;

import com.rounds.goodies.eventpipe.RoundsBroadcastListener;
import com.rounds.goodies.eventpipe.RoundsEventCommonHandler;

/**
 * A base class for activities that want to handle events using RoundsEventCommonHandler
 */
public abstract class RoundsActivityBase extends Activity implements RoundsBroadcastListener {

	private RoundsEventCommonHandler mCommonEventHandler;

	@Override
	protected void onResume() {
		super.onResume();
		if (getRoundsEventInterests() != null) {
			if (mCommonEventHandler == null) {
				mCommonEventHandler = new RoundsEventCommonHandler(this, this);
			}
			mCommonEventHandler.registerRoundsEventReceivers();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (getRoundsEventInterests() != null) {
			if (mCommonEventHandler != null) {
				mCommonEventHandler.unregisterRoundsEventReceivers();
			}
		}
	}

	public void addInterestToHandler(String event) {
		mCommonEventHandler.addAndRegisterEvent(event);
	}
}
