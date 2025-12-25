package net.rossonet.waldot.btree.task.branch;

import java.util.List;

import net.rossonet.waldot.api.btree.Task;
import net.rossonet.waldot.btree.SingleRunningChildBranch;

/**
 * A {@code Selector} is a branch task that runs every children until one of
 * them succeeds. If a child task fails, the selector will start and run the
 * next child task.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author implicit-invocation
 */
public class Selector extends SingleRunningChildBranch {
	/**
	 * Creates a {@code Selector} branch with the given children.
	 * 
	 * @param tasks the children of this task
	 */
	public Selector(final List<Task> tasks) {
		super(tasks);
	}

	/**
	 * Creates a {@code Selector} branch with the given children.
	 * 
	 * @param tasks the children of this task
	 */
	public Selector(final Task... tasks) {
		super(List.of(tasks));
	}

	@Override
	public void notifyChildFail(final Task runningTask) {
		super.notifyChildFail(runningTask);
		if (++currentChildIndex < children.size()) {
			tick(); // Run next child
		} else {
			doFail(); // All children processed, return failure status
		}
	}

	@Override
	public void notifyChildSuccess(final Task runningTask) {
		super.notifyChildSuccess(runningTask);
		notifySuccess(); // Return success status when a child says it succeeded
	}

}
