package net.rossonet.waldot.btree;

import java.util.concurrent.Semaphore;

import net.rossonet.waldot.api.btree.Task;
import net.rossonet.waldot.api.btree.WaldotBehaviorTreesEngine;
import net.rossonet.waldot.api.models.WaldotNamespace;

public class DefaultBehaviorTreeEngine implements WaldotBehaviorTreesEngine {

	public DefaultBehaviorTreeEngine(final WaldotNamespace waldotNamespace) {
		// TODO Auto-generated constructor stub
	}
//FIXME: completare la runtime BehaviorTrees

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public Task createRootTask(final String subtree) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createSemaphore(final String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public Semaphore getSemaphore(final String name) {
		// TODO Auto-generated method stub
		return null;
	}
}
