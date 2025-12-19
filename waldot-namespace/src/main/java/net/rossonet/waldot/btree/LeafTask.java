package net.rossonet.waldot.btree;

import net.rossonet.waldot.api.btree.Task;

/**
 * A {@code LeafTask} is a terminal task of a behavior tree, contains action or
 * condition logic, can not have any child.
 * 
 * @param <E> type of the blackboard object that tasks use to read or modify
 *            game state
 * 
 * @author implicit-invocation
 * @author davebaol
 */
public abstract class LeafTask extends AbstractTask {

	/** Creates a leaf task. */
	public LeafTask() {
	}

	/**
	 * Always throws {@code IllegalStateException} because a leaf task cannot have
	 * any children.
	 */
	@Override
	public int addChildToTask(final Task child) {
		throw new IllegalStateException("A leaf task cannot have any children");
	}

	@Override
	public final void childFail(final Task runningTask) {
	}

	@Override
	public final void childRunning(final Task runningTask, final Task reporter) {
	}

	@Override
	public final void childSuccess(final Task runningTask) {
	}

	/**
	 * This method contains the update logic of this leaf task. The actual
	 * implementation MUST return one of {@link Status#RUNNING} ,
	 * {@link Status#SUCCEEDED} or {@link Status#FAILED}. Other return values will
	 * cause an {@code IllegalStateException}.
	 * 
	 * @return the status of this leaf task
	 */
	public abstract Status execute();

	@Override
	public Task getChild(final int i) {
		throw new IndexOutOfBoundsException("A leaf task can not have any child");
	}

	@Override
	public int getChildCount() {
		return 0;
	}

	/**
	 * This method contains the update logic of this task. The implementation
	 * delegates the {@link #execute()} method.
	 */
	@Override
	public final void run() {
		final Status result = execute();
		if (result == null) {
			throw new IllegalStateException("Invalid status 'null' returned by the execute method");
		}
		switch (result) {
		case SUCCEEDED:
			success();
			return;
		case FAILED:
			fail();
			return;
		case RUNNING:
			running();
			return;
		default:
			throw new IllegalStateException("Invalid status '" + result.name() + "' returned by the execute method");
		}
	}

}
