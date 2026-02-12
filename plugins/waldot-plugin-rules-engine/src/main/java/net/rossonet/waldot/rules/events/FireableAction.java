package net.rossonet.waldot.rules.events;

public abstract class FireableAction implements Runnable {

	private long startingTime;

	public long getStartingTime() {
		return startingTime;
	}

	public void setStartingTime(long timeMillis) {
		startingTime = timeMillis;
	}

}
