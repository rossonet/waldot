package net.rossonet.waldot.btree.task.branch;

import java.util.ArrayList;
import java.util.List;

import net.rossonet.waldot.api.btree.Task;
import net.rossonet.waldot.btree.AbstractTask;
import net.rossonet.waldot.btree.BranchTask;

/**
 * A {@code Parallel} is a special branch task that runs all children when
 * stepped. Its actual behavior depends on its {@link orchestrator} and
 * {@link policy}.<br>
 * <br>
 * The execution of the parallel task's children depends on its
 * {@link #orchestrator}:
 * <ul>
 * <li>{@link Orchestrator#Resume}: the parallel task restarts or runs each
 * child every step</li>
 * <li>{@link Orchestrator#Join}: child tasks will run until success or failure
 * but will not re-run until the parallel task has succeeded or failed</li>
 * </ul>
 * 
 * The actual result of the parallel task depends on its {@link #policy}:
 * <ul>
 * <li>{@link Policy#Sequence}: the parallel task fails as soon as one child
 * fails; if all children succeed, then the parallel task succeeds. This is the
 * default policy.</li>
 * <li>{@link Policy#Selector}: the parallel task succeeds as soon as one child
 * succeeds; if all children fail, then the parallel task fails.</li>
 * </ul>
 * 
 * The typical use case: make the game entity react on event while sleeping or
 * wandering.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author implicit-invocation
 * @author davebaol
 */
public class Parallel extends BranchTask {

	/**
	 * The enumeration of the child orchestrators supported by the {@link Parallel}
	 * task
	 */
	public enum Orchestrator {
		/**
		 * Children execute until they succeed or fail but will not re-run until the
		 * parallel task has succeeded or failed
		 */
		Join() {
			@Override
			public void execute(final Parallel parallel) {
				parallel.noRunningTasks = true;
				parallel.lastResult = null;
				for (parallel.currentChildIndex = 0; parallel.currentChildIndex < parallel.children
						.size(); parallel.currentChildIndex++) {
					final Task child = parallel.children.get(parallel.currentChildIndex);

					switch (child.getStatus()) {
					case RUNNING:
						child.tick();
						break;
					case SUCCEEDED:
					case FAILED:
						break;
					default:
						child.setControl(parallel);
						child.doStart();
						child.tick();
						break;
					}

					if (parallel.lastResult != null) { // Current child has finished either with success or fail
						parallel.cancelRunningChildren(parallel.noRunningTasks ? parallel.currentChildIndex + 1 : 0);
						parallel.resetAllChildren();
						if (parallel.lastResult) {
							parallel.notifySuccess();
						} else {
							parallel.doFail();
						}
						return;
					}
				}
				parallel.notifyRunning();
			}
		},
		/**
		 * The default orchestrator - starts or resumes all children every single step
		 */
		Resume() {
			@Override
			public void execute(final Parallel parallel) {
				parallel.noRunningTasks = true;
				parallel.lastResult = null;
				for (parallel.currentChildIndex = 0; parallel.currentChildIndex < parallel.children
						.size(); parallel.currentChildIndex++) {
					final Task child = parallel.children.get(parallel.currentChildIndex);
					if (child.getStatus() == Status.RUNNING) {
						child.tick();
					} else {
						child.setControl(parallel);
						child.doStart();

						child.tick();
					}

					if (parallel.lastResult != null) { // Current child has finished either with success or fail
						parallel.cancelRunningChildren(parallel.noRunningTasks ? parallel.currentChildIndex + 1 : 0);
						if (parallel.lastResult) {
							parallel.notifySuccess();
						} else {
							parallel.doFail();
						}
						return;
					}
				}
				parallel.notifyRunning();
			}
		};

		/**
		 * Called by parallel task each run
		 * 
		 * @param parallel The {@link Parallel} task
		 */
		public abstract void execute(Parallel parallel);
	}

	/** The enumeration of the policies supported by the {@link Parallel} task. */
	public enum Policy {
		/**
		 * The selector policy makes the {@link Parallel} task succeed as soon as one
		 * child succeeds; if all children fail, then the parallel task fails.
		 */
		Selector() {
			@Override
			public Boolean onChildFail(final Parallel parallel) {
				return parallel.noRunningTasks && parallel.currentChildIndex == parallel.children.size() - 1
						? Boolean.FALSE
						: null;
			}

			@Override
			public Boolean onChildSuccess(final Parallel parallel) {
				return Boolean.TRUE;
			}
		},
		/**
		 * The sequence policy makes the {@link Parallel} task fail as soon as one child
		 * fails; if all children succeed, then the parallel task succeeds. This is the
		 * default policy.
		 */
		Sequence() {
			@Override
			public Boolean onChildFail(final Parallel parallel) {
				return Boolean.FALSE;
			}

			@Override
			public Boolean onChildSuccess(final Parallel parallel) {
				switch (parallel.orchestrator) {
				case Join:
					return parallel.noRunningTasks
							&& parallel.children.get(parallel.children.size() - 1).getStatus() == Status.SUCCEEDED
									? Boolean.TRUE
									: null;
				case Resume:
				default:
					return parallel.noRunningTasks && parallel.currentChildIndex == parallel.children.size() - 1
							? Boolean.TRUE
							: null;
				}
			}
		};

		/**
		 * Called by parallel task each time one of its children fails.
		 * 
		 * @param parallel the parallel task
		 * @return {@code Boolean.TRUE} if parallel must succeed, {@code Boolean.FALSE}
		 *         if parallel must fail and {@code null} if parallel must keep on
		 *         running.
		 */
		public abstract Boolean onChildFail(Parallel parallel);

		/**
		 * Called by parallel task each time one of its children succeeds.
		 * 
		 * @param parallel the parallel task
		 * @return {@code Boolean.TRUE} if parallel must succeed, {@code Boolean.FALSE}
		 *         if parallel must fail and {@code null} if parallel must keep on
		 *         running.
		 */
		public abstract Boolean onChildSuccess(Parallel parallel);

	}

	private int currentChildIndex;
	private Boolean lastResult;
	private boolean noRunningTasks;

	/**
	 * Optional task attribute specifying the execution policy (defaults to
	 * {@link Orchestrator#Resume})
	 */
	public Orchestrator orchestrator;

	/**
	 * Optional task attribute specifying the parallel policy (defaults to
	 * {@link Policy#Sequence})
	 */
	public Policy policy;

	/**
	 * Creates a parallel task with sequence policy, resume orchestrator and no
	 * children
	 */
	public Parallel() {
		this(new ArrayList<>());
	}

	/**
	 * Creates a parallel task with sequence policy, resume orchestrator and the
	 * given children
	 * 
	 * @param tasks the children
	 */
	public Parallel(final List<Task> tasks) {
		this(Policy.Sequence, tasks);
	}

	/**
	 * Creates a parallel task with the given orchestrator, sequence policy and the
	 * given children
	 * 
	 * @param orchestrator the orchestrator
	 * @param tasks        the children
	 */
	public Parallel(final Orchestrator orchestrator, final List<Task> tasks) {
		this(Policy.Sequence, orchestrator, tasks);
	}

	/**
	 * Creates a parallel task with the given orchestrator, sequence policy and the
	 * given children
	 * 
	 * @param orchestrator the orchestrator
	 * @param tasks        the children
	 */
	public Parallel(final Orchestrator orchestrator, final Task... tasks) {
		this(Policy.Sequence, orchestrator, List.of(tasks));
	}

	/**
	 * Creates a parallel task with the given policy, resume orchestrator and no
	 * children
	 * 
	 * @param policy the policy
	 */
	public Parallel(final Policy policy) {
		this(policy, new ArrayList<>());
	}

	/**
	 * Creates a parallel task with the given policy, resume orchestrator and the
	 * given children
	 * 
	 * @param policy the policy
	 * @param tasks  the children
	 */
	public Parallel(final Policy policy, final AbstractTask... tasks) {
		this(policy, List.of(tasks));
	}

	/**
	 * Creates a parallel task with the given policy, resume orchestrator and the
	 * given children
	 * 
	 * @param policy the policy
	 * @param tasks  the children
	 */
	public Parallel(final Policy policy, final List<Task> tasks) {
		this(policy, Orchestrator.Resume, tasks);
	}

	/**
	 * Creates a parallel task with the given orchestrator, policy and children
	 * 
	 * @param policy       the policy
	 * @param orchestrator the orchestrator
	 * @param tasks        the children
	 */
	public Parallel(final Policy policy, final Orchestrator orchestrator, final List<Task> tasks) {
		super(tasks);
		this.policy = policy;
		this.orchestrator = orchestrator;
		noRunningTasks = true;
	}

	/**
	 * Creates a parallel task with sequence policy, resume orchestrator and the
	 * given children
	 * 
	 * @param tasks the children
	 */
	public Parallel(final Task... tasks) {
		this(List.of(tasks));
	}

	@Override
	public void doResetTask() {
		super.doResetTask();
		policy = Policy.Sequence;
		orchestrator = Orchestrator.Resume;
		noRunningTasks = true;
		lastResult = null;
		currentChildIndex = 0;
	}

	@Override
	public void notifyChildFail(final Task runningTask) {
		lastResult = policy.onChildFail(this);
	}

	@Override
	public void notifyChildRunning(final Task task) {
		noRunningTasks = false;
	}

	@Override
	public void notifyChildSuccess(final Task runningTask) {
		lastResult = policy.onChildSuccess(this);
	}

	public void resetAllChildren() {
		for (int i = 0, n = getChildCount(); i < n; i++) {
			final Task child = getChild(i);
			child.doResetTask();
		}
	}

	@Override
	public void tick() {
		orchestrator.execute(this);
	}
}
