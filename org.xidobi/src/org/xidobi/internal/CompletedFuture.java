package org.xidobi.internal;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.xidobi.internal.Preconditions.checkArgumentNotNull;

public class CompletedFuture<T> implements Future<T> {

		private final T value;

		private final IOException exception;

		/**
		 * 
		 */
		private CompletedFuture(T value, IOException exception) {
			this.value = value;
			this.exception = exception;
		}
		
		public static <T> CompletedFuture<T> completedWithException(IOException e){
			checkArgumentNotNull(e, "e");
			return new CompletedFuture<T>(null, e);
		}
		public static <T> CompletedFuture<T> completedWithValue(T value){
			checkArgumentNotNull(value, "value");
			return new CompletedFuture<T>(value, null);
		}

		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		public boolean isCancelled() {
			return false;
		}

		public boolean isDone() {
			return true;
		}

		public T get() throws ExecutionException {
			if (value!=null)
				return value;
			throw new ExecutionException(exception);
		}

		public T get(long timeout, TimeUnit unit) throws ExecutionException {
			return get();
		}
	}