package net.rossonet.waldot.utils;

import java.lang.Thread.Builder.OfVirtual;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.netty.channel.nio.NioEventLoopGroup;

/**
 * ThreadHelper is a utility class that provides methods to run tasks with a
 * timeout. It allows you to execute a Callable or Runnable with a specified
 * timeout, throwing an exception if the task does not complete within the
 * allotted time.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public final class ThreadHelper {

	/**
	 * Creates a new NioEventLoopGroup using virtual threads.
	 * 
	 * @param name the base name for the thread factory
	 * @return a new NioEventLoopGroup configured with virtual threads
	 */
	public static NioEventLoopGroup newVirtualEventLoopGroup(String name) {
		return new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(),
				Thread.ofVirtual().name(name, 0).factory());
	}

	/**
	 * Creates a new ScheduledExecutorService using virtual threads.
	 * 
	 * @param name the base name for the thread factory
	 * @return a new ScheduledExecutorService configured with virtual threads
	 */
	public static ScheduledExecutorService newVirtualSchedulerExecutor(String name) {
		return Executors.newScheduledThreadPool(Thread.activeCount(), Thread.ofVirtual().name(name, 0).factory());
	}

	/**
	 * Creates a new ExecutorService that uses virtual threads for each task.
	 * 
	 * @return a new ExecutorService using virtual thread per task execution
	 */
	public static ExecutorService newVirtualThreadExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}

	/**
	 * Returns a Thread.Builder for creating virtual threads.
	 * 
	 * @return a new Thread.Builder configured for virtual threads
	 */
	public static OfVirtual ofVirtual() {
		return Thread.ofVirtual();
	}

	/**
	 * Executes a Callable with a timeout. If the callable does not complete within
	 * the specified time, the task is cancelled and a TimeoutException is thrown.
	 * 
	 * @param <RETURN_TYPE> the return type of the callable
	 * @param callable     the callable to execute
	 * @param timeout      the maximum time to wait
	 * @param timeUnit     the unit of the timeout
	 * @return the result returned by the callable
	 * @throws Exception if the callable throws an exception, or if timeout occurs
	 */
	public static <RETURN_TYPE> RETURN_TYPE runWithTimeout(final Callable<RETURN_TYPE> callable, final long timeout,
			final TimeUnit timeUnit) throws Exception {
		final ExecutorService executor = newVirtualThreadExecutor();
		final Future<RETURN_TYPE> future = executor.submit(callable);
		executor.shutdown();
		try {
			return future.get(timeout, timeUnit);
		} catch (final TimeoutException e) {
			future.cancel(true);
			throw e;
		} catch (final ExecutionException e) {
			final Throwable t = e.getCause();
			if (t instanceof Error) {
				throw (Error) t;
			} else if (t instanceof Exception) {
				throw e;
			} else {
				throw new IllegalStateException(t);
			}
		}
	}

	/**
	 * Executes a Runnable with a timeout. If the runnable does not complete within
	 * the specified time, the task is cancelled and a TimeoutException is thrown.
	 * 
	 * @param runnable     the runnable to execute
	 * @param timeout      the maximum time to wait
	 * @param timeUnit     the unit of the timeout
	 * @throws Exception if an exception occurs during execution, or if timeout occurs
	 */
	public static void runWithTimeout(final Runnable runnable, final long timeout, final TimeUnit timeUnit)
			throws Exception {
		runWithTimeout(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				runnable.run();
				return null;
			}
		}, timeout, timeUnit);
	}

	private ThreadHelper() {
		throw new UnsupportedOperationException("Just for static usage");

	}

}
