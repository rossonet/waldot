package net.rossonet.waldot.btree;

import java.util.ArrayList;
import java.util.List;

import net.rossonet.waldot.api.btree.Task;

//(minChildren = 1)
public abstract class BranchTask extends AbstractTask {

	/** The children of this branch task. */
	protected List<Task> children;

	/** Create a branch task with no children */
	public BranchTask() {
		this(new ArrayList<>());
	}

	/**
	 * Create a branch task with a list of children
	 * 
	 * @param tasks list of this task's children, can be empty
	 */
	public BranchTask(final List<Task> tasks) {
		this.children = tasks;
	}

	@Override
	public int addChildToTask(final Task child) {
		children.add(child);
		return children.size() - 1;
	}

	@Override
	public Task getChild(final int i) {
		return children.get(i);
	}

	@Override
	public int getChildCount() {
		return children.size();
	}

	@Override
	public void resetTask() {
		children.clear();
		super.resetTask();
	}

}
