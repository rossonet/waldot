package net.rossonet.waldot.btree.task.decorator;

import net.rossonet.waldot.api.btree.Task;
import net.rossonet.waldot.btree.Decorator;

/**
 * The {@code Random} decorator succeeds with the specified probability,
 * regardless of whether the wrapped task fails or succeeds. Also, the wrapped
 * task is optional, meaning that this decorator can act like a leaf task.
 * <p>
 * Notice that if success probability is 1 this task is equivalent to the
 * decorator {@link AlwaysSucceed} and the leaf {@link Success}. Similarly if
 * success probability is 0 this task is equivalent to the decorator
 * {@link AlwaysFail} and the leaf {@link Failure}.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author davebaol
 */
//(minChildren = 0, maxChildren = 1)
public class Random extends Decorator {

	private static final float ZERO_POINT_FIVE = 0.5f;

	/**
	 * Optional task attribute specifying the random distribution that determines
	 * the success probability. It defaults to
	 * {@link ConstantFloatDistribution#ZERO_POINT_FIVE}.
	 */
	public float success;

	/**
	 * Creates a {@code Random} decorator with no child that succeeds or fails with
	 * equal probability.
	 */
	public Random() {
		this(ZERO_POINT_FIVE);
	}

	/**
	 * Creates a {@code Random} decorator with no child that succeeds with the
	 * specified probability.
	 * 
	 * @param success the random distribution that determines success probability
	 */
	public Random(final float success) {
		super();
		this.success = success;
	}

	/**
	 * Creates a {@code Random} decorator with the given child that succeeds with
	 * the specified probability.
	 * 
	 * @param success the random distribution that determines success probability
	 * @param task    the child task to wrap
	 */
	public Random(final float success, final Task task) {
		super(task);
		this.success = success;
	}

	/**
	 * Creates a {@code Random} decorator with the given child that succeeds or
	 * fails with equal probability.
	 * 
	 * @param task the child task to wrap
	 */
	public Random(final Task task) {
		this(ZERO_POINT_FIVE, task);
	}

	private void decide() {
		final float randomNumber = (float) Math.random();
		if (randomNumber <= success) {
			notifySuccess();
		} else {
			doFail();
		}
	}

	@Override
	public void doResetTask() {
		this.success = ZERO_POINT_FIVE;
		super.doResetTask();
	}

	/**
	 * Draws a value from the distribution that determines the success probability.
	 * <p>
	 * This method is called when the task is entered.
	 */
	@Override
	public void doStart() {
		// No action needed since success is a fixed float value
	}

	@Override
	public void notifyChildFail(final Task runningTask) {
		decide();
	}

	@Override
	public void notifyChildSuccess(final Task runningTask) {
		decide();
	}

	@Override
	public void tick() {
		if (child() != null) {
			child().tick();
		} else {
			decide();
		}
	}
}
