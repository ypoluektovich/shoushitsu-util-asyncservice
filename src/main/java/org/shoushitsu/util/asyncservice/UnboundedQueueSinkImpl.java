package org.shoushitsu.util.asyncservice;

import java.util.Queue;

public final class UnboundedQueueSinkImpl implements TaskSinkImplementation {

	private final Queue<Task<?>> tasks;

	public UnboundedQueueSinkImpl(Queue<Task<?>> tasks) {
		this.tasks = tasks;
	}

	@Override
	public final boolean offer(Task<?> task) {
		return tasks.offer(task);
	}

}
