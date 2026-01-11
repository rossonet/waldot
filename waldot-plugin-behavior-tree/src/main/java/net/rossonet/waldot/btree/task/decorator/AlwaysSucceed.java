package net.rossonet.waldot.btree.task.decorator;

import net.rossonet.waldot.api.btree.Task;
import net.rossonet.waldot.btree.AbstractTask;
import net.rossonet.waldot.btree.Decorator;

/**
 * An {@code AlwaysSucceed} decorator will succeed no matter the wrapped task
 * succeeds or fails.
 * 
 */
public class AlwaysSucceed extends Decorator {

	/**
	 * Creates an {@code AlwaysSucceed} decorator with the given child.
	 * 
	 * @param task the child task to wrap
	 */
	public AlwaysSucceed(final AbstractTask task) {
		super(task);
	}

	@Override
	public void notifyChildFail(final Task runningTask) {
		notifyChildSuccess(runningTask);
	}

}
