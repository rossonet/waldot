/*******************************************************************************
 * Copyright 2014 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package net.rossonet.waldot.btree;

import java.util.ArrayList;
import java.util.List;

import net.rossonet.waldot.api.btree.Task;

/**
 * The behavior tree itself.
 * 
 * @param type of the blackboard object that tasks use to read or modify game
 *             state
 * 
 * @author implicit-invocation
 * @author davebaol
 */
public class BehaviorTree extends AbstractTask {

	/**
	 * The listener interface for receiving task events. The class that is
	 * interested in processing a task event implements this interface, and the
	 * object created with that class is registered with a behavior tree, using the
	 * {@link BehaviorTree#addListener(Listener)} method. When a task event occurs,
	 * the corresponding method is invoked.
	 *
	 * @param type of the blackboard object that tasks use to read or modify game
	 *             state
	 * 
	 * @author davebaol
	 */
	public interface Listener {

		/**
		 * This method is invoked when a child task is added to the children of a parent
		 * task.
		 * 
		 * @param task  the parent task of the newly added child
		 * @param index the index where the child has been added
		 */
		public void childAdded(Task task, int index);

		/**
		 * This method is invoked when the task status is set. This does not necessarily
		 * mean that the status has changed.
		 * 
		 * @param task           the task whose status has been set
		 * @param previousStatus the task's status before the update
		 */
		public void statusUpdated(Task task, Status previousStatus);
	}

	public List<Listener> listeners;

	private Task rootTask;

	/**
	 * Creates a {@code BehaviorTree} with no root task and no blackboard object.
	 * Both the root task and the blackboard object must be set before running this
	 * behavior tree, see {@link #addChild(Task) addChild()} and
	 * {@link #setObject(Object) setObject()} respectively.
	 */
	public BehaviorTree() {
		this(null);
	}

	/**
	 * Creates a behavior tree with a root task and a blackboard object. Both the
	 * root task and the blackboard object must be set before running this behavior
	 * tree, see {@link #addChild(Task) addChild()} and {@link #setObject(Object)
	 * setObject()} respectively.
	 * 
	 * @param rootTask the root task of this tree. It can be {@code null}.
	 * @param object   the blackboard. It can be {@code null}.
	 */
	public BehaviorTree(final Task rootTask) {
		this.rootTask = rootTask;
	}

	/**
	 * This method will add a child, namely the root, to this behavior tree.
	 * 
	 * @param child the root task to add
	 * @return the index where the root task has been added (always 0).
	 * @throws IllegalStateException if the root task is already set.
	 */
	@Override
	public int addChild(final Task child) {
		if (this.rootTask != null) {
			throw new IllegalStateException("A behavior tree cannot have more than one root task");
		}
		this.rootTask = child;
		return 0;
	}

	public void addListener(final Listener listener) {
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		listeners.add(listener);
	}

	@Override
	public void doResetTask() {
		removeListeners();
		this.rootTask = null;
		this.listeners = null;
		super.doResetTask();
	}

	@Override
	public Task getChild(final int i) {
		if (i == 0 && rootTask != null) {
			return rootTask;
		}
		throw new IndexOutOfBoundsException("index can't be >= size: " + i + " >= " + getChildCount());
	}

	@Override
	public int getChildCount() {
		return rootTask == null ? 0 : 1;
	}

	public void notifyChildAdded(final Task task, final int index) {
		for (final Listener listener : listeners) {
			listener.childAdded(task, index);
		}
	}

	@Override
	public void notifyChildFail(final Task runningTask) {
		doFail();
	}

	@Override
	public void notifyChildRunning(final Task runningTask) {
		notifyRunning();
	}

	@Override
	public void notifyChildSuccess(final Task runningTask) {
		notifySuccess();
	}

	public void notifyStatusUpdated(final Task task, final Status previousStatus) {
		for (final Listener listener : listeners) {
			listener.statusUpdated(task, previousStatus);
		}
	}

	public void removeListener(final Listener listener) {
		if (listeners != null) {
			listeners.remove(listener);
		}
	}

	public void removeListeners() {
		if (listeners != null) {
			listeners.clear();
		}
	}

	/**
	 * This method should be called when game entity needs to make decisions: call
	 * this in game loop or after a fixed time slice if the game is real-time, or on
	 * entity's turn if the game is turn-based
	 */
	public void step() {
		if (rootTask.getStatus() == Status.RUNNING) {
			rootTask.tick();
		} else {
			rootTask.setControl(this);
			rootTask.doStart();
			rootTask.tick();
		}
	}

	@Override
	public void tick() {
	}
}
