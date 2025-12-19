package net.rossonet.waldot.btree.task.decorator;

import net.rossonet.waldot.api.btree.Task;
import net.rossonet.waldot.btree.LoopDecorator;

/**
 * A {@code Repeat} decorator will repeat the wrapped task a certain number of
 * times, possibly infinite. This task always succeeds when reaches the
 * specified number of repetitions.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author implicit-invocation
 */
public class Repeat extends LoopDecorator {

	private int count;

	/**
	 * Optional task attribute specifying the integer distribution that determines
	 * how many times the wrapped task must be repeated. Defaults to
	 * {@link ConstantIntegerDistribution#NEGATIVE_ONE} which indicates an infinite
	 * number of repetitions.
	 * 
	 * @see #start()
	 */
	public int times;

	/** Creates an infinite repeat decorator with no child task. */
	public Repeat() {
		this(null);
	}

	/**
	 * Creates a repeat decorator that executes the given task the number of times
	 * (possibly infinite) determined by the given distribution. The number of times
	 * is drawn from the distribution by the {@link #start()} method. Any negative
	 * value means forever.
	 * 
	 * @param times the integer distribution specifying how many times the child
	 *              must be repeated.
	 * @param child the task that will be wrapped
	 */
	public Repeat(final int times, final Task child) {
		super(child);
		this.times = times;
	}

	/**
	 * Creates an infinite repeat decorator that wraps the given task.
	 * 
	 * @param child the task that will be wrapped
	 */
	public Repeat(final Task child) {
		this(0, child);
	}

	@Override
	public void childFail(final Task runningTask) {
		childSuccess(runningTask);
	}

	@Override
	public void childSuccess(final Task runningTask) {
		if (count > 0) {
			count--;
		}
		if (count == 0) {
			super.childSuccess(runningTask);
			loop = false;
		} else {
			loop = true;
		}
	}

	@Override
	public boolean condition() {
		return loop && count != 0;
	}

	@Override
	public void resetTask() {
		count = 0;
		times = 0;
		super.resetTask();
	}

	/**
	 * Draws a value from the distribution that determines how many times the
	 * wrapped task must be repeated. Any negative value means forever.
	 * <p>
	 * This method is called when the task is entered.
	 */
	@Override
	public void start() {
		count = 0;
	}
}
