package net.rossonet.waldot.btree.task.branch;

import java.util.List;

import net.rossonet.waldot.api.btree.Task;

/**
 * A {@code RandomSequence} is a sequence task's variant that runs its children
 * in a random order.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author implicit-invocation
 */
public class RandomSequence extends Sequence {

	/** Creates a {@code RandomSequence} branch with no children. */
	public RandomSequence() {
		super();
	}

	/**
	 * Creates a {@code RandomSequence} branch with the given children.
	 * 
	 * @param tasks the children of this task
	 */
	public RandomSequence(final List<Task> tasks) {
		super(tasks);
	}

	/**
	 * Creates a {@code RandomSequence} branch with the given children.
	 * 
	 * @param tasks the children of this task
	 */
	public RandomSequence(final Task... tasks) {
		super(tasks);
	}

	@Override
	public void start() {
		super.start();
		if (randomChildren == null) {
			randomChildren = createRandomChildren();
		}
	}
}
