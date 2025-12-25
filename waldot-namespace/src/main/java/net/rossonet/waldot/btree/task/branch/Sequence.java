package net.rossonet.waldot.btree.task.branch;

import java.util.List;

import net.rossonet.waldot.api.btree.Task;
import net.rossonet.waldot.btree.SingleRunningChildBranch;

/**
 * A {@code Sequence} is a branch task that runs every children until one of
 * them fails. If a child task succeeds, the selector will start and run the
 * next child task.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author implicit-invocation
 */
public class Sequence extends SingleRunningChildBranch {

	/**
	 * Creates a {@code Sequence} branch with the given children.
	 * 
	 * @param tasks the children of this task
	 */
	public Sequence(final List<Task> tasks) {
		super(tasks);
	}

	/**
	 * Creates a {@code Sequence} branch with the given children.
	 * 
	 * @param tasks the children of this task
	 */
	public Sequence(final Task... tasks) {
		super(List.of(tasks));
	}

	@Override
	public void notifyChildFail(final Task runningTask) {
		super.notifyChildFail(runningTask);
		doFail(); // Return failure status when a child says it failed
	}

	@Override
	public void notifyChildSuccess(final Task runningTask) {
		super.notifyChildSuccess(runningTask);
		if (++currentChildIndex < children.size()) {
			tick(); // Run next child
		} else {
			notifySuccess(); // All children processed, return success status
		}
	}

}
