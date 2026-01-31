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

	/** Whether the {@link #tick()} method must keep looping or not. */
	protected boolean loop;

	/**
	 * Creates a loop decorator that wraps the given task.
	 * 
	 * @param child the task that will be wrapped
	 */
	public LoopDecorator(final Task child) {
		super(child);
	}

	/**
	 * Whether the {@link #tick()} method must keep looping or not.
	 * 
	 * @return {@code true} if it must keep looping; {@code false} otherwise.
	 */
	public boolean condition() {
		return loop;
	}

	@Override
	public void doResetTask() {
		loop = false;
		super.doResetTask();
	}

	@Override
	public void notifyChildRunning(final Task runningTask) {
		super.notifyChildRunning(runningTask);
		loop = false;
	}

	@Override
	public void tick() {
		loop = true;
		while (condition()) {
			if (child().getStatus() == Status.RUNNING) {
				child().tick();
			} else {
				child().setControl(this);
				child().doStart();
				child().tick();
			}
		}
	}

}
