package net.rossonet.waldot.btree;

import java.util.List;

import net.rossonet.waldot.api.btree.Task;

/**
 * A {@code SingleRunningChildBranch} task is a branch task that supports only
 * one running child at a time.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author implicit-invocation
 * @author davebaol
 */
public abstract class SingleRunningChildBranch extends BranchTask {

	/** The index of the child currently processed. */
	protected int currentChildIndex;

	/**
	 * Array of random children. If it's {@code null} this task is deterministic.
	 */
	protected Task[] randomChildren;

	/** The child in the running status or {@code null} if no child is running. */
	protected Task runningChild;

	/**
	 * Creates a {@code SingleRunningChildBranch} task with a list of children
	 * 
	 * @param tasks list of this task's children, can be empty
	 */
	public SingleRunningChildBranch(final List<Task> tasks) {
		super(tasks);
	}

	@Override
	protected void cancelRunningChildren(final int startIndex) {
		super.cancelRunningChildren(startIndex);
		runningChild = null;
	}

	protected AbstractTask[] createRandomChildren() {
		final AbstractTask[] rndChildren = new AbstractTask[children.size()];
		System.arraycopy(children, 0, rndChildren, 0, children.size());
		return rndChildren;
	}

	@Override
	public void doResetTask() {
		super.doResetTask();
		this.currentChildIndex = 0;
		this.runningChild = null;
		this.randomChildren = null;
	}

	@Override
	public void doStart() {
		this.currentChildIndex = 0;
		runningChild = null;
	}

	@Override
	public void notifyChildFail(final Task task) {
		this.runningChild = null;
	}

	@Override
	public void notifyChildRunning(final Task task) {
		runningChild = task;
		notifyRunning(); // Return a running status when a child says it's running
	}

	@Override
	public void notifyChildSuccess(final Task task) {
		this.runningChild = null;
	}

	@Override
	public void tick() {
		if (runningChild != null) {
			runningChild.tick();
		} else {
			if (currentChildIndex < children.size()) {
				if (randomChildren != null) {
					final int last = children.size() - 1;
					if (currentChildIndex < last) {
						// Random swap
						final int otherChildIndex = currentChildIndex
								+ (int) (Math.random() * (last - currentChildIndex + 1));
						final Task tmp = randomChildren[currentChildIndex];
						randomChildren[currentChildIndex] = randomChildren[otherChildIndex];
						randomChildren[otherChildIndex] = tmp;
					}
					runningChild = randomChildren[currentChildIndex];
				} else {
					runningChild = children.get(currentChildIndex);
				}
				runningChild.setControl(this);
				runningChild.doStart();
				runningChild.tick();
			} else {
				// Should never happen; this case must be handled by subclasses in childXXX
				// methods
			}
		}
	}

}
