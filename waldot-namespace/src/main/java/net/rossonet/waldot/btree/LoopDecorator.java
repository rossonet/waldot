package net.rossonet.waldot.btree;

import net.rossonet.waldot.api.btree.Task;

/**
 * {@code LoopDecorator} is an abstract class providing basic functionalities
 * for concrete looping decorators.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author davebaol
 */
public abstract class LoopDecorator extends Decorator {

	/** Whether the {@link #run()} method must keep looping or not. */
	protected boolean loop;

	/** Creates a loop decorator with no child task. */
	public LoopDecorator() {
	}

	/**
	 * Creates a loop decorator that wraps the given task.
	 * 
	 * @param child the task that will be wrapped
	 */
	public LoopDecorator(final Task child) {
		super(child);
	}

	@Override
	public void childRunning(final Task runningTask, final Task reporter) {
		super.childRunning(runningTask, reporter);
		loop = false;
	}

	/**
	 * Whether the {@link #run()} method must keep looping or not.
	 * 
	 * @return {@code true} if it must keep looping; {@code false} otherwise.
	 */
	public boolean condition() {
		return loop;
	}

	@Override
	public void resetTask() {
		loop = false;
		super.resetTask();
	}

	@Override
	public void run() {
		loop = true;
		while (condition()) {
			if (child.getStatus() == Status.RUNNING) {
				child.run();
			} else {
				child.setControl(this);
				child.start();
				child.run();
			}
		}
	}

}
