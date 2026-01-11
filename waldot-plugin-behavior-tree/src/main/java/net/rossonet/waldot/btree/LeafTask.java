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
	public int addChild(final Task child) {
		throw new IllegalStateException("A leaf task cannot have any children");
	}

	/**
	 * This method contains the update logic of this leaf task. The actual
	 * implementation MUST return one of {@link Status#RUNNING} ,
	 * {@link Status#SUCCEEDED} or {@link Status#FAILED}. Other return values will
	 * cause an {@code IllegalStateException}.
	 * 
	 * @return the status of this leaf task
	 */
	public abstract Status executeTick();

	@Override
	public Task getChild(final int i) {
		throw new IndexOutOfBoundsException("A leaf task can not have any child");
	}

	@Override
	public int getChildCount() {
		return 0;
	}

	@Override
	public final void notifyChildFail(final Task runningTask) {
	}

	@Override
	public final void notifyChildRunning(final Task runningTask) {
	}

	@Override
	public final void notifyChildSuccess(final Task runningTask) {
	}

	/**
	 * This method contains the update logic of this task. The implementation
	 * delegates the {@link #executeTick()} method.
	 */
	@Override
	public final void tick() {
		final Status result = executeTick();
		if (result == null) {
			throw new IllegalStateException("Invalid status 'null' returned by the execute method");
		}
		switch (result) {
		case SUCCEEDED:
			notifySuccess();
			return;
		case FAILED:
			doFail();
			return;
		case RUNNING:
			notifyRunning();
			return;
		default:
			throw new IllegalStateException("Invalid status '" + result.name() + "' returned by the execute method");
		}
	}

}
