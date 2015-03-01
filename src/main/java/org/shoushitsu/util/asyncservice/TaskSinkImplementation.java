package org.shoushitsu.util.asyncservice;

public interface TaskSinkImplementation {
	boolean offer(Task<?> task);
}
