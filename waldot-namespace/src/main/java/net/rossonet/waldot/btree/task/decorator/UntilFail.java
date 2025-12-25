package net.rossonet.waldot.btree.task.decorator;

import net.rossonet.waldot.api.btree.Task;
import net.rossonet.waldot.btree.AbstractTask;
import net.rossonet.waldot.btree.LoopDecorator;

/**
 * The {@code UntilFail} decorator will repeat the wrapped task until that task
 * fails, which makes the decorator succeed.
 * <p>
 * Notice that a wrapped task that always succeeds without entering the running
 * status will cause an infinite loop in the current frame.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author implicit-invocation
 * @author davebaol
 */
public class UntilFail extends LoopDecorator {

	/**
	 * Creates an {@code UntilFail} decorator with the given child.
	 * 
	 * @param task the child task to wrap
	 */
	public UntilFail(final AbstractTask task) {
		super(task);
	}

	@Override
	public void notifyChildFail(final Task runningTask) {
		notifySuccess();
		loop = false;
	}

	@Override
	public void notifyChildSuccess(final Task runningTask) {
		loop = true;
	}
}
