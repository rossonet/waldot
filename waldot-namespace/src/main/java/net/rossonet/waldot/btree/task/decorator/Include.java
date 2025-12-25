package net.rossonet.waldot.btree.task.decorator;

import net.rossonet.waldot.api.btree.Task;
import net.rossonet.waldot.api.btree.WaldotBehaviorTreesEngine;
import net.rossonet.waldot.btree.Decorator;

/**
 * An {@code Include} decorator grafts a subtree. When the subtree is grafted
 * depends on the value of the {@link #lazy} attribute: at clone-time if is
 * {@code false}, at run-time if is {@code true}.
 *
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 *
 * @author davebaol
 * @author implicit-invocation
 */
//(minChildren = 0, maxChildren = 0)
public class Include extends Decorator {

	/**
	 * Optional task attribute indicating whether the subtree should be included at
	 * clone-time ({@code false}, the default) or at run-time ({@code true}).
	 */
	public boolean lazy;

	/** Mandatory task attribute indicating the path of the subtree to include. */
	// (required = true)
	public String subtree;

	private final WaldotBehaviorTreesEngine engine;

	/**
	 * Creates a non-lazy {@code Include} decorator without specifying the subtree.
	 */
	public Include(final WaldotBehaviorTreesEngine engine) {
		super();
		this.engine = engine;
	}

	/**
	 * Creates a non-lazy {@code Include} decorator for the specified subtree.
	 * 
	 * @param subtree the subtree reference, usually a path
	 */
	public Include(final WaldotBehaviorTreesEngine engine, final String subtree) {
		this.engine = engine;
		this.subtree = subtree;
	}

	/**
	 * Creates an eager or lazy {@code Include} decorator for the specified subtree.
	 * 
	 * @param subtree the subtree reference, usually a path
	 * @param lazy    whether inclusion should happen at clone-time (false) or at
	 *                run-time (true)
	 */
	public Include(final WaldotBehaviorTreesEngine engine, final String subtree, final boolean lazy) {
		this.engine = engine;
		this.subtree = subtree;
		this.lazy = lazy;
	}

	private Task createSubtreeRootTask() {
		final Task rootTask = engine.createRootTask(subtree);
		return rootTask;
	}

	@Override
	public void doResetTask() {
		lazy = false;
		subtree = null;
		super.doResetTask();
	}

	/**
	 * The first call of this method lazily sets its child to the referenced subtree
	 * created through the {@link BehaviorTreeLibraryManager}. Subsequent calls do
	 * nothing since the child has already been set. A
	 * {@link UnsupportedOperationException} is thrown if this {@code Include} is
	 * eager.
	 *
	 * @throws UnsupportedOperationException if this {@code Include} is eager
	 */
	@Override
	public void doStart() {
		if (!lazy) {
			throw new UnsupportedOperationException(
					"A non-lazy " + Include.class.getSimpleName() + " isn't meant to be run!");
		}

		if (child() == null) {
			// Lazy include is grafted at run-time
			addChild(createSubtreeRootTask());
		}
	}

	public WaldotBehaviorTreesEngine getEngine() {
		return engine;
	}
}
