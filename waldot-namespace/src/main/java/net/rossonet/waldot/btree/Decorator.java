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

	/** The child task wrapped by this decorator */
	protected Task child;

	/** Creates a decorator with no child task. */
	public Decorator() {
	}

	/**
	 * Creates a decorator that wraps the given task.
	 * 
	 * @param child the task that will be wrapped
	 */
	public Decorator(final Task child) {
		this.child = child;
	}

	@Override
	public int addChild(final Task child) {
		if (this.child != null) {
			throw new IllegalStateException("A decorator task cannot have more than one child");
		}
		this.child = child;
		return 0;
	}

	@Override
	public int addChildToTask(final Task child) {
		throw new UnsupportedOperationException("A decorator task cannot have more than one child");
	}

	@Override
	public void childFail(final Task runningTask) {
		fail();
	}

	@Override
	public void childRunning(final Task runningTask, final Task reporter) {
		running();
	}

	@Override
	public void childSuccess(final Task runningTask) {
		success();
	}

	@Override
	public Task getChild(final int i) {
		if (i == 0 && child != null) {
			return child;
		}
		throw new IndexOutOfBoundsException("index can't be >= size: " + i + " >= " + getChildCount());
	}

	@Override
	public int getChildCount() {
		return child == null ? 0 : 1;
	}

	@Override
	public void resetTask() {
		child = null;
		super.resetTask();
	}

	@Override
	public void run() {
		if (child.getStatus() == Status.RUNNING) {
			child.run();
		} else {
			child.setControl(this);
			child.start();
			child.run();
		}
	}

}
