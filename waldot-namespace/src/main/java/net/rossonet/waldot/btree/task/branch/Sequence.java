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

	/** Creates a {@code Sequence} branch with no children. */
	public Sequence() {
		super();
	}

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
	public void childFail(final Task runningTask) {
		super.childFail(runningTask);
		fail(); // Return failure status when a child says it failed
	}

	@Override
	public void childSuccess(final Task runningTask) {
		super.childSuccess(runningTask);
		if (++currentChildIndex < children.size()) {
			run(); // Run next child
		} else {
			success(); // All children processed, return success status
		}
	}

}
