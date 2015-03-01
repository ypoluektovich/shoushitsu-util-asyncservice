package org.shoushitsu.util.asyncservice;

import java.util.Arrays;
import java.util.List;

public final class BoundedArrayQueue {

	private final Task<?>[] tasks;

	private int head;

	private int size;

	BoundedArrayQueue(int externalCapacityLog2) {
		if (externalCapacityLog2 < 0 || externalCapacityLog2 > 30) {
			throw new IllegalArgumentException("bad externalCapacityLog2: " + externalCapacityLog2);
		}
		tasks = new Task<?>[1 << externalCapacityLog2];
		head = 0;
		size = 0;
	}

	public final boolean isEmpty() {
		return size == 0;
	}

	public final boolean offer(Task<?> task) {
		if (size == tasks.length) {
			return false;
		}
		tasks[(head + size++) & (tasks.length - 1)] = task;
		return true;
	}

	public final Task<?> poll() {
		if (size == 0) {
			return null;
		}
		Task<?> task = tasks[head];
		tasks[head] = null;
		head = (head + 1) & (tasks.length - 1);
		--size;
		return task;
	}

	final List<Task<?>> drain() {
		Task<?>[] tasksCopy = new Task<?>[size];
		int beforeWrap = Math.min(size, tasks.length - head);
		System.arraycopy(tasks, head, tasksCopy, 0, beforeWrap);
		if (beforeWrap != size) {
			System.arraycopy(tasks, 0, tasksCopy, beforeWrap, size - beforeWrap);
		}
		head = 0;
		size = 0;
		Arrays.fill(tasks, 0, tasks.length, null);
		return Arrays.asList(tasksCopy);
	}

}
