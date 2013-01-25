/*
 * Copyright Gemtec GmbH 2009-2013
 *
 * Erstellt am: 25.01.2013 11:02:44
 * Erstellt von: Christian Schwarz 
 */
package org.xidobi.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class PendingFuture<T> implements Future<T> {
	private volatile boolean isCanceled = false;
	private volatile T value;

	private final Lock lock = new ReentrantLock();

	private final Condition valueIsSet = lock.newCondition();

	public boolean cancel(boolean mayInterruptIfRunning) {
		if (value!=null)
			return false;
		isCanceled=true;
		return false;
	}

	public boolean isCancelled() {
		return isCanceled;
	}

	public boolean isDone() {
		return false;
	}

	public T get() throws InterruptedException, ExecutionException {
		lock.lock();
		try {
			if (value!=null)
				return value;
			valueIsSet.await();
			return value;
		}
		finally {
			lock.unlock();
		}
	}

	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		lock.lock();		
		try {
			if (value!=null)
				return value;
			if (!valueIsSet.await(timeout, unit))
				throw new TimeoutException();
			return value;
		}
		finally {
			lock.unlock();
		}
	}

	protected void set(T value) {
		lock.lock();
		try{
			if (this.value!=null)
				throw new IllegalStateException("A value is already set!");
			
			this.value = value;
			valueIsSet.signalAll();
			
		}finally{
			lock.unlock();
		}
	}

}