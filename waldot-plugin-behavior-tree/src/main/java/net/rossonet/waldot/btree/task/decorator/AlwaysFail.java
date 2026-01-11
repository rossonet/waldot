package net.rossonet.waldot.btree.task.decorator;

import net.rossonet.waldot.api.btree.Task;
import net.rossonet.waldot.btree.AbstractTask;
import net.rossonet.waldot.btree.Decorator;

/**
 * An {@code AlwaysFail} decorator will fail no matter the wrapped task fails or
 * succeeds.
 * 
 */
public class AlwaysFail extends Decorator {

	/**
	 * Creates an {@code AlwaysFail} decorator with the given child.
	 * 
	 * @param task the child task to wrap
	 */
	public AlwaysFail(final AbstractTask task) {
		super(task);
	}

	@Override
	public void notifyChildSuccess(final Task runningTask) {
		notifyChildFail(runningTask);
	}

}
