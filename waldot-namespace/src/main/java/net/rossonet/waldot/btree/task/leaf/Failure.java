package net.rossonet.waldot.btree.task.leaf;

import net.rossonet.waldot.btree.LeafTask;

/**
 * {@code Failure} is a leaf that immediately fails.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author davebaol
 */
public class Failure extends LeafTask {

	/** Creates a {@code Failure} task. */
	public Failure() {
	}

	/**
	 * Executes this {@code Failure} task.
	 * 
	 * @return {@link Status#FAILED}.
	 */
	@Override
	public Status execute() {
		return Status.FAILED;
	}

}
