package net.rossonet.waldot.api.btree;

public interface Task extends AutoCloseable {

	/**
	 * The enumeration of the values that a task's status can have.
	 * 
	 */
	public enum Status {
		/** Means that the task has been terminated by an ancestor. */
		CANCELLED,
		/** Means that the task returned a failure result. */
		FAILED,
		/** Means that the task has never run or has been reset. */
		FRESH,
		/** Means that the task needs to run again. */
		RUNNING,
		/** Means that the task returned a success result. */
		SUCCEEDED;
	}

	/**
	 * This method will add a child to the list of this task's children
	 * 
	 * @param child the child task which will be added
	 * @return the index where the child has been added.
	 * @throws IllegalStateException if the child cannot be added for whatever
	 *                               reason.
	 */
	int addChild(Task child);

	/**
	 * Terminates this task and all its running children. This method MUST be called
	 * only if this task is running.
	 */
	void cancel();

	/**
	 * This method will be called when one of the children of this task fails
	 * 
	 * @param task the task that failed
	 */
	void childFail(Task task);

	/**
	 * This method will be called when one of the ancestors of this task needs to
	 * run again
	 * 
	 * @param runningTask the task that needs to run again
	 * @param reporter    the task that reports, usually one of this task's children
	 */
	void childRunning(Task runningTask, Task reporter);

	/**
	 * This method will be called when one of the children of this task succeeds
	 * 
	 * @param task the task that succeeded
	 */
	void childSuccess(Task task);

	/**
	 * This method will be called by {@link #success()}, {@link #fail()} or
	 * {@link #cancel()}, meaning that this task's status has just been set to
	 * {@link Status#SUCCEEDED}, {@link Status#FAILED} or {@link Status#CANCELLED}
	 * respectively.
	 */
	void end();

	/**
	 * This method will be called in {@link #run()} to inform control that this task
	 * has finished running with a failure result
	 */
	void fail();

	Status getPreviousStatus();

	Status getStatus();

	/** Resets this task to make it restart from scratch on next run. */
	void resetTask();

	void run();

	/**
	 * This method will be called in {@link #run()} to inform control that this task
	 * needs to run again
	 */
	void running();

	/**
	 * This method will set a task as this task's control (parent)
	 * 
	 * @param control the parent task
	 */
	void setControl(Task control);

	/** This method will be called once before this task's first run. */
	void start();

	/**
	 * This method will be called in {@link #run()} to inform control that this task
	 * has finished running with a success result
	 */
	void success();

}
