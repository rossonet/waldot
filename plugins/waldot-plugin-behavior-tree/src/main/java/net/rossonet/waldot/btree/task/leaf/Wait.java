package net.rossonet.waldot.btree.task.leaf;

import net.rossonet.waldot.btree.LeafTask;

/**
 * {@code Wait} is a leaf that keeps running for the specified amount of time
 * then succeeds.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author davebaol
 */
public class Wait extends LeafTask {

	/**
	 * Mandatory task attribute specifying the random distribution that determines
	 * the timeout in seconds.
	 */
	public long seconds;

	private float startTime;

	/** Creates a {@code Wait} task that immediately succeeds. */
	public Wait() {
		this(0l);
	}

	/**
	 * Creates a {@code Wait} task running for the specified number of seconds.
	 * 
	 * @param seconds the random distribution determining the number of seconds to
	 *                wait for
	 */
	public Wait(final long seconds) {
		this.seconds = seconds;
	}

	@Override
	public void doResetTask() {
		seconds = 0l;
		startTime = 0;
		super.doResetTask();
	}

	/**
	 * Draws a value from the distribution that determines the seconds to wait for.
	 * <p>
	 * This method is called when the task is entered. Also, this method internally
	 * calls {@link Timepiece#getTime() GdxAI.getTimepiece().getTime()} to get the
	 * current AI time. This means that
	 * <ul>
	 * <li>if you forget to {@link Timepiece#update(float) update the timepiece}
	 * this task will keep running indefinitely.</li>
	 * <li>the timepiece should be updated before this task runs.</li>
	 * </ul>
	 */
	@Override
	public void doStart() {
		startTime = System.currentTimeMillis() / 1000f;
	}

	/**
	 * Executes this {@code Wait} task.
	 * 
	 * @return {@link Status#SUCCEEDED} if the specified timeout has expired;
	 *         {@link Status#RUNNING} otherwise.
	 */
	@Override
	public Status executeTick() {
		return (System.currentTimeMillis() / 1000f) - startTime < seconds ? Status.RUNNING : Status.SUCCEEDED;
	}

}
