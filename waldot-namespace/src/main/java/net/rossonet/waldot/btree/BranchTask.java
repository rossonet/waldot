package net.rossonet.waldot.btree;

import java.util.List;

import net.rossonet.waldot.api.btree.Task;

//(minChildren = 1)
public abstract class BranchTask extends AbstractTask {

	/**
	 * Create a branch task with a list of children
	 * 
	 * @param tasks list of this task's children, can be empty
	 */
	public BranchTask(final List<Task> tasks) {
		super(tasks);
	}

}
