package net.rossonet.waldot.btree.task.decorator;

import java.util.concurrent.Semaphore;

import net.rossonet.waldot.api.btree.Task;
import net.rossonet.waldot.api.btree.WaldotBehaviorTreesEngine;
import net.rossonet.waldot.btree.Decorator;

/**
 * A {@code SemaphoreGuard} decorator allows you to specify how many characters
 * should be allowed to concurrently execute its child which represents a
 * limited resource used in different behavior trees (note that this does not
 * necessarily involve multithreading concurrency).
 * <p>
 * This is a simple mechanism for ensuring that a limited shared resource is not
 * over subscribed. You might have a pool of 5 pathfinders, for example, meaning
 * at most 5 characters can be pathfinding at a time. Or you can associate a
 * semaphore to the player character to ensure that at most 3 enemies can
 * simultaneously attack him.
 * <p>
 * This decorator fails when it cannot acquire the semaphore. This allows a
 * selector task higher up the tree to find a different action that doesn't
 * involve the contested resource.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author davebaol
 */
public class SemaphoreGuard extends Decorator {

	/** Mandatory task attribute specifying the semaphore name. */
	// (required = true)
	public String name;

	private boolean semaphoreAcquired;

	private WaldotBehaviorTreesEngine engine;

	/**
	 * Creates a {@code SemaphoreGuard} decorator with the specified semaphore name
	 * and child.
	 * 
	 * @param name the semaphore name
	 * @param task the child task to wrap
	 */
	public SemaphoreGuard(final String name, final Task task) {
		super(task);
		this.name = name;
	}

	/** Creates a {@code SemaphoreGuard} decorator with no child. */
	public SemaphoreGuard(final WaldotBehaviorTreesEngine engine) {
		this.engine = engine;
	}

	/**
	 * Creates a {@code SemaphoreGuard} decorator with no child the specified
	 * semaphore name.
	 * 
	 * @param name the semaphore name
	 */
	public SemaphoreGuard(final WaldotBehaviorTreesEngine engine, final String name) {
		this.name = name;
		this.engine = engine;
	}

	/**
	 * Creates a {@code SemaphoreGuard} decorator with the given child.
	 * 
	 * @param task the child task to wrap
	 */
	public SemaphoreGuard(final WaldotBehaviorTreesEngine engine, final Task task) {
		super(task);
		this.engine = engine;
	}

	/**
	 * Releases the semaphore.
	 * <p>
	 * This method is called when the task exits.
	 */
	@Override
	public void doEnd() {
		if (semaphoreAcquired) {
			if (semaphore() != null) {
				semaphore().release();
			}
			semaphoreAcquired = false;
		}
		super.doEnd();
	}

	@Override
	public void doResetTask() {
		name = null;
		semaphoreAcquired = false;
		super.doResetTask();
	}

	/**
	 * Acquires the semaphore. Also, the first execution of this method retrieves
	 * the semaphore by name and stores it locally.
	 * <p>
	 * This method is called when the task is entered.
	 */
	@Override
	public void doStart() {
		if (semaphore() == null) {
			engine.createSemaphore(name);
		}
		try {
			semaphore().acquire();
		} catch (final InterruptedException e) {
			doFail();
		}
		super.doStart();
	}

	public WaldotBehaviorTreesEngine getEngine() {
		return engine;
	}

	/**
	 * Runs its child if the semaphore has been successfully acquired; immediately
	 * fails otherwise.
	 */
	@Override
	public void tick() {
		if (semaphoreAcquired) {
			super.tick();
		} else {
			doFail();
		}
	}

	private Semaphore semaphore() {
		return engine.getSemaphore(name);
	}

}
