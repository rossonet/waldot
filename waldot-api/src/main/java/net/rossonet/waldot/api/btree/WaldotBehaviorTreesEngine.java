package net.rossonet.waldot.api.btree;

import java.util.concurrent.Semaphore;

public interface WaldotBehaviorTreesEngine extends AutoCloseable {

	Task createRootTask(String subtree);

	void createSemaphore(String name);

	Semaphore getSemaphore(String name);

}