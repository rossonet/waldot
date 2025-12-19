package net.rossonet.waldot.btree.task.branch;

import java.util.List;

import net.rossonet.waldot.api.btree.Task;

/**
 * A {@code RandomSelector} is a selector task's variant that runs its children
 * in a random order.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author implicit-invocation
 */
public class RandomSelector extends Selector {

	/** Creates a {@code RandomSelector} branch with no children. */
	public RandomSelector() {
		super();
	}

	/**
	 * Creates a {@code RandomSelector} branch with the given children.
	 * 
	 * @param tasks the children of this task
	 */
	public RandomSelector(final List<Task> tasks) {
		super(tasks);
	}

	/**
	 * Creates a {@code RandomSelector} branch with the given children.
	 * 
	 * @param tasks the children of this task
	 */
	public RandomSelector(final Task... tasks) {
		super(List.of(tasks));
	}

	@Override
	public void start() {
		super.start();
		if (randomChildren == null) {
			randomChildren = createRandomChildren();
		}
	}
}
