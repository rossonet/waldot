package net.rossonet.waldot.btree;

import net.rossonet.waldot.api.btree.Task;

/**
 * A {@code Decorator} is a wrapper that provides custom behavior for its child.
 * The child can be of any kind (branch task, leaf task, or another decorator).
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author implicit-invocation
 * @author davebaol
 */
//(minChildren = 1, maxChildren = 1)
public abstract class Decorator extends AbstractTask {

	public Decorator() {
		super();
	}

	/**
	 * Creates a decorator that wraps the given task.
	 * 
	 * @param child the task that will be wrapped
	 */
	public Decorator(final Task child) {
		super();
		addChild(child);
	}

	@Override
	public int addChild(final Task child) {
		if (this.children.size() > 0) {
			throw new IllegalStateException("A decorator task cannot have more than one child");
		}

		return 0;
	}

	protected Task child() {
		if (children.isEmpty()) {
			return null;
		}
		return children.get(0);
	}

	@Override
	public void doResetTask() {
		super.doResetTask();
	}

	@Override
	public Task getChild(final int i) {
		if (i == 0 && child() != null) {
			return child();
		}
		throw new IndexOutOfBoundsException("index can't be >= size: " + i + " >= " + getChildCount());
	}

	@Override
	public int getChildCount() {
		return child() == null ? 0 : 1;
	}

	@Override
	public void notifyChildFail(final Task runningTask) {
		doFail();
	}

	@Override
	public void notifyChildRunning(final Task runningTask) {
		notifyRunning();
	}

	@Override
	public void notifyChildSuccess(final Task runningTask) {
		notifySuccess();
	}

	@Override
	public void tick() {
		if (child().getStatus() == Status.RUNNING) {
			child().tick();
		} else {
			child().setControl(this);
			child().doStart();
			child().tick();
		}
	}

}
