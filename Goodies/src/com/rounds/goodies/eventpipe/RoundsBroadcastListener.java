package com.rounds.goodies.eventpipe;

import android.os.Bundle;

public interface RoundsBroadcastListener {
	
	public void handleRoundsEvent(final String action, final Bundle extras);
	
	public String[] getRoundsEventInterests();
}
