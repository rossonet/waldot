package net.rossonet.waldot.btree;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.btree.Task;

public abstract class AbstractTask implements Task {

	private static final Logger logger = LoggerFactory.getLogger(AbstractTask.class);
	/** The parent of this task */
	protected Task control;

	/** The status of this task. */
	protected Status status = Status.FRESH;

	private Status previousStatus;

	/** The children of this task. */
	protected List<Task> children;

	/** Create a task with no children */
	public AbstractTask() {
		this(new ArrayList<>());
	}

	/**
	 * Create a task with a list of children
	 * 
	 * @param tasks list of this task's children, can be empty
	 */
	public AbstractTask(final List<Task> tasks) {
		this.children = tasks;
	}

	/**
	 * This method will add a child to the list of this task's children
	 * 
	 * @param child the child task which will be added
	 * @return the index where the child has been added.
	 * @throws IllegalStateException if the child cannot be added for whatever
	 *                               reason.
	 */
	@Override
	public synchronized int addChild(final Task child) {
		children.add(child);
		final int index = children.size() - 1;
		logger.debug("Added child {} to task {} at index {}", child, this, index);
		return index;
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
				child.doCancel();
			} else {
				logger.debug("Child {} of task {} is not running, skipping cancel", child, this);
			}
		}
	}

	@Override
	public void close() throws Exception {
		doCancel();
	}

	/**
	 * Terminates this task and all its running children. This method MUST be called
	 * only if this task is running.
	 */
	@Override
	public final void doCancel() {
		cancelRunningChildren(0);
		previousStatus = status;
		status = Status.CANCELLED;
		doEnd();
	}

	/**
	 * This method will be called by {@link #notifySuccess()}, {@link #doFail()} or
	 * {@link #doCancel()}, meaning that this task's status has just been set to
	 * {@link Status#SUCCEEDED}, {@link Status#FAILED} or {@link Status#CANCELLED}
	 * respectively.
	 */
	@Override
	public void doEnd() {
		cancelRunningChildren(0);
		logger.debug("Task {} ended with status {}", this, status);
	}

	/**
	 * This method will be called in {@link #tick()} to inform control that this
	 * task has finished running with a failure result
	 */
	@Override
	public final void doFail() {
		previousStatus = status;
		status = Status.FAILED;
		doEnd();
		control.notifyChildFail(this);

	}

	/** Resets this task to make it restart from scratch on next run. */
	@Override
	public void doResetTask() {
		if (status == Status.RUNNING) {
			doCancel();
		}
		for (int i = 0, n = getChildCount(); i < n; i++) {
			getChild(i).doResetTask();
		}
		status = Status.FRESH;
	}

	@Override
	/** This method will be called once before this task's first run. */
	public void doStart() {
		logger.debug("Task {} started", this);
	}

	/** Returns the child at the given index. */
	public Task getChild(final int i) {
		return children.get(i);
	}

	/**
	 * Returns the number of children of this task.
	 * 
	 * @return an int giving the number of children of this task
	 */
	public int getChildCount() {
		return children.size();
	}

	@Override
	public Status getPreviousStatus() {
		return previousStatus;
	}

	/** Returns the status of this task. */
	@Override
	public final Status getStatus() {
		return status;
	}

	/**
	 * This method will be called when one of the children of this task fails
	 * 
	 * @param task the task that failed
	 */
	@Override
	public abstract void notifyChildFail(Task task);

	/**
	 * This method will be called when one of the children of this task succeeds
	 * 
	 * @param task the task that succeeded
	 */
	@Override
	public abstract void notifyChildSuccess(Task task);

	/**
	 * This method will be called in {@link #tick()} to inform control that this
	 * task needs to run again
	 */
	@Override
	public final void notifyRunning() {
		previousStatus = status;
		status = Status.RUNNING;
		if (control != null) {
			control.notifyChildRunning(this);
		}
	}

	/**
	 * This method will be called in {@link #tick()} to inform control that this
	 * task has finished running with a success result
	 */
	@Override
	public final void notifySuccess() {
		previousStatus = status;
		status = Status.SUCCEEDED;
		doEnd();
		if (control != null) {
			control.notifyChildSuccess(this);
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

	/**
	 * This method contains the update logic of this task. The actual implementation
	 * MUST call {@link #notifyRunning()}, {@link #notifySuccess()} or
	 * {@link #doFail()} exactly once.
	 */
	@Override
	public abstract void tick();

}
