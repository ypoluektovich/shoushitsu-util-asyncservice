package org.shoushitsu.util.asyncservice;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public final class UnboundedTaskQueue extends ATaskQueue {

	private final Queue<Task<?>> tasks = new LinkedList<>();

	public final TaskSink sink;

	public UnboundedTaskQueue() {
		sink = createSink(new UnboundedQueueSinkImpl(tasks));
	}

	@Override
	protected boolean isEmpty() {
		return tasks.isEmpty();
	}

	@Override
	protected Task<?> poll() {
		return tasks.poll();
	}

	@Override
	protected final void drainTo(Collection<Task<?>> sink) {
		lock.lock();
		try {
			sink.addAll(tasks);
			tasks.clear();
		} finally {
			lock.unlock();
		}
	}

}
