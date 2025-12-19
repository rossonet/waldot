package net.rossonet.waldot.btree;

import net.rossonet.waldot.api.btree.Task;

public abstract class AbstractTask implements Task {

	/** The parent of this task */
	protected Task control;

	/** The status of this task. */
	protected Status status = Status.FRESH;

	private Status previousStatus;

	/**
	 * This method will add a child to the list of this task's children
	 * 
	 * @param child the child task which will be added
	 * @return the index where the child has been added.
	 * @throws IllegalStateException if the child cannot be added for whatever
	 *                               reason.
	 */
	@Override
	public int addChild(final Task child) {
		final int index = addChildToTask(child);
		return index;
	}

	/**
	 * This method will add a child to the list of this task's children
	 * 
	 * @param child the child task which will be added
	 * @return the index where the child has been added.
	 * @throws IllegalStateException if the child cannot be added for whatever
	 *                               reason.
	 */
	public abstract int addChildToTask(Task child);

	/**
	 * Terminates this task and all its running children. This method MUST be called
	 * only if this task is running.
	 */
	@Override
	public final void cancel() {
		cancelRunningChildren(0);
		previousStatus = status;
		status = Status.CANCELLED;
		end();
	}

	/**
	 * Terminates the running children of this task starting from the specified
	 * index up to the end.
	 * 
	 * @param startIndex the start index
	 */
	protected void cancelRunningChildren(final int startIndex) {
		for (int i = startIndex, n = getChildCount(); i < n; i++) {
			final Task child = getChild(i);
			if (child.getStatus() == Status.RUNNING) {
				child.cancel();
			}
		}
	}

	/**
	 * This method will be called when one of the children of this task fails
	 * 
	 * @param task the task that failed
	 */
	@Override
	public abstract void childFail(Task task);

	/**
	 * This method will be called when one of the ancestors of this task needs to
	 * run again
	 * 
	 * @param runningTask the task that needs to run again
	 * @param reporter    the task that reports, usually one of this task's children
	 */
	@Override
	public abstract void childRunning(Task runningTask, Task reporter);

	/**
	 * This method will be called when one of the children of this task succeeds
	 * 
	 * @param task the task that succeeded
	 */
	@Override
	public abstract void childSuccess(Task task);

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * This method will be called by {@link #success()}, {@link #fail()} or
	 * {@link #cancel()}, meaning that this task's status has just been set to
	 * {@link Status#SUCCEEDED}, {@link Status#FAILED} or {@link Status#CANCELLED}
	 * respectively.
	 */
	@Override
	public void end() {
	}

	/**
	 * This method will be called in {@link #run()} to inform control that this task
	 * has finished running with a failure result
	 */
	@Override
	public final void fail() {
		previousStatus = status;
		status = Status.FAILED;
		end();
		if (control != null) {
			control.childFail(this);
		}
	}

	/** Returns the child at the given index. */
	public abstract Task getChild(int i);

	/**
	 * Returns the number of children of this task.
	 * 
	 * @return an int giving the number of children of this task
	 */
	public abstract int getChildCount();

	@Override
	public Status getPreviousStatus() {
		return previousStatus;
	}

	/** Returns the status of this task. */
	@Override
	public final Status getStatus() {
		return status;
	}

	/** Resets this task to make it restart from scratch on next run. */
	@Override
	public void resetTask() {
		if (status == Status.RUNNING) {
			cancel();
		}
		for (int i = 0, n = getChildCount(); i < n; i++) {
			getChild(i).resetTask();
		}
		status = Status.FRESH;
		control = null;
	}

	/**
	 * This method contains the update logic of this task. The actual implementation
	 * MUST call {@link #running()}, {@link #success()} or {@link #fail()} exactly
	 * once.
	 */
	@Override
	public abstract void run();

	/**
	 * This method will be called in {@link #run()} to inform control that this task
	 * needs to run again
	 */
	@Override
	public final void running() {
		previousStatus = status;
		status = Status.RUNNING;
		if (control != null) {
			control.childRunning(this, this);
		}
	}

	/**
	 * This method will set a task as this task's control (parent)
	 * 
	 * @param control the parent task
	 */
	@Override
	public final void setControl(final Task control) {
		this.control = control;
	}

	@Override
	/** This method will be called once before this task's first run. */
	public void start() {
	}

	/**
	 * This method will be called in {@link #run()} to inform control that this task
	 * has finished running with a success result
	 */
	@Override
	public final void success() {
		previousStatus = status;
		status = Status.SUCCEEDED;
		end();
		if (control != null) {
			control.childSuccess(this);
		}
	}

}
