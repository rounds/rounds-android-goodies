package com.rounds.goodies;

import android.app.Fragment;
import android.os.Bundle;

import com.rounds.goodies.eventpipe.RoundsBroadcastListener;
import com.rounds.goodies.eventpipe.RoundsEventCommonHandler;

/**
 * A base class for fragments that want to handle events using RoundsEventCommonHandler
 */
public abstract class RoundsFragmentBase extends Fragment implements RoundsBroadcastListener{
	
	private RoundsEventCommonHandler mCommonEventHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if ( getRoundsEventInterests() != null ){
			mCommonEventHandler = new RoundsEventCommonHandler(this.getActivity().getBaseContext(), this);
			mCommonEventHandler.registerRoundsEventReceivers();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if ( getRoundsEventInterests() != null ){
			mCommonEventHandler.unregisterRoundsEventReceivers();
			mCommonEventHandler = null;
		}
	}	
	
	public void addInterestToHandler(String event) {
		mCommonEventHandler.addAndRegisterEvent(event);
	}
}
