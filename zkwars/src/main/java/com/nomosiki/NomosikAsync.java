package com.nomosiki;

import net.minecraft.server.level.ServerLevel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Small thread pool for work that must finish on the server thread (entity mutations,
 * world access). Same idea as zk-recruit's {@code AsyncManager} + {@code AsyncTaskWithCallback}.
 */
public final class NomosikAsync {
	public static final int THREAD_COUNT = Math.max(2, Integer.getInteger("nomosiki.asyncThreads", 2));

	private static final AtomicInteger THREAD_NO = new AtomicInteger(1);
	private static final ThreadFactory FACTORY = runnable -> {
		Thread t = new Thread(runnable, "nomosiki-async-" + THREAD_NO.getAndIncrement());
		t.setDaemon(true);
		return t;
	};
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(THREAD_COUNT, FACTORY);

	private NomosikAsync() {
	}

	public static <T> void run(ServerLevel level, Supplier<T> work, Consumer<T> onMainThread) {
		EXECUTOR.execute(() -> {
			T result = work.get();
			level.getServer().execute(() -> onMainThread.accept(result));
		});
	}
}
