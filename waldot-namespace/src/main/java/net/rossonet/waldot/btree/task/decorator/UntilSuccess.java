package net.rossonet.waldot.btree.task.decorator;

import net.rossonet.waldot.api.btree.Task;
import net.rossonet.waldot.btree.LoopDecorator;

/**
 * The {@code UntilSuccess} decorator will repeat the wrapped task until that
 * task succeeds, which makes the decorator succeed.
 * <p>
 * Notice that a wrapped task that always fails without entering the running
 * status will cause an infinite loop in the current frame.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author implicit-invocation
 * @author davebaol
 */
public class UntilSuccess extends LoopDecorator {

	/** Creates an {@code UntilSuccess} decorator with no child. */
	public UntilSuccess() {
	}

	/**
	 * Creates an {@code UntilSuccess} decorator with the given child.
	 * 
	 * @param task the child task to wrap
	 */
	public UntilSuccess(final Task task) {
		super(task);
	}

	@Override
	public void childFail(final Task runningTask) {
		loop = true;
	}

	@Override
	public void childSuccess(final Task runningTask) {
		success();
		loop = false;
	}
}
