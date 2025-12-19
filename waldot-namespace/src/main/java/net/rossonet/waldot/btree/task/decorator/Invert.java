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

	/** Creates an {@code Invert} decorator with no child. */
	public Invert() {
	}

	/**
	 * Creates an {@code Invert} decorator with the given child.
	 * 
	 * @param task the child task to wrap
	 */
	public Invert(final AbstractTask task) {
		super(task);
	}

	@Override
	public void childFail(final Task runningTask) {
		super.childSuccess(runningTask);
	}

	@Override
	public void childSuccess(final Task runningTask) {
		super.childFail(runningTask);
	}

}
