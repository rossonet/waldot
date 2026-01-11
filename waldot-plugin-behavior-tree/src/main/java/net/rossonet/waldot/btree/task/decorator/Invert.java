package net.rossonet.waldot.btree.task.decorator;

import net.rossonet.waldot.api.btree.Task;
import net.rossonet.waldot.btree.AbstractTask;
import net.rossonet.waldot.btree.Decorator;

/**
 * An {@code Invert} decorator will succeed if the wrapped task fails and will
 * fail if the wrapped task succeeds.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author implicit-invocation
 */
public class Invert extends Decorator {
	/**
	 * Creates an {@code Invert} decorator with the given child.
	 * 
	 * @param task the child task to wrap
	 */
	public Invert(final AbstractTask task) {
		super(task);
	}

	@Override
	public void notifyChildFail(final Task runningTask) {
		super.notifyChildSuccess(runningTask);
	}

	@Override
	public void notifyChildSuccess(final Task runningTask) {
		super.notifyChildFail(runningTask);
	}

}
