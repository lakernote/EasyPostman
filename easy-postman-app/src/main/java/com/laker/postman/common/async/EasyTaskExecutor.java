package com.laker.postman.common.async;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 异步任务执行器
 * <p>
 * 用于在后台线程执行耗时操作（如IO、网络请求等），然后在EDT线程中更新UI。
 * 避免在EDT中执行耗时操作导致界面卡顿。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 示例1：简单的后台任务 + UI更新
 * AsyncTaskExecutor.execute(
 *     () -> {
 *         // 后台线程：执行耗时的IO操作
 *         return loadDataFromFile();
 *     },
 *     result -> {
 *         // EDT线程：使用加载结果更新UI
 *         updateUIWithData(result);
 *     },
 *     error -> {
 *         // EDT线程：处理错误
 *         showErrorMessage(error.getMessage());
 *     }
 * );
 *
 * // 示例2：只执行后台任务，不需要返回值
 * AsyncTaskExecutor.execute(
 *     () -> saveDataToFile(data),
 *     () -> showSuccessMessage(),
 *     error -> showErrorMessage(error.getMessage())
 * );
 *
 * // 示例3：使用自定义线程池和线程名
 * AsyncTaskExecutor.builder()
 *     .threadName("CustomTask")
 *     .backgroundTask(() -> heavyComputation())
 *     .onSuccess(result -> updateUI(result))
 *     .onError(error -> handleError(error))
 *     .execute();
 * }</pre>
 *
 * @author Laker
 */
@Slf4j
@UtilityClass
public class EasyTaskExecutor {

    private static final String DEFAULT_THREAD_NAME = "EasyTask";
    private static final String ERROR_MESSAGE = "Error executing async task";

    /**
     * 默认线程池：用于执行异步任务
     * 核心线程数为CPU核心数，最大线程数为CPU核心数的2倍
     */
    private static final ExecutorService DEFAULT_EXECUTOR = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactory() {
                private int count = 0;

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, DEFAULT_THREAD_NAME + "-" + (++count));
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 执行异步任务（无返回值）
     *
     * @param backgroundTask 后台任务（在后台线程执行）
     * @param onSuccess      成功回调（在EDT线程执行）
     * @param onError        错误回调（在EDT线程执行）
     */
    public static void execute(
            Runnable backgroundTask,
            Runnable onSuccess,
            Consumer<Exception> onError
    ) {
        execute(backgroundTask, onSuccess, onError, DEFAULT_THREAD_NAME);
    }

    /**
     * 执行异步任务（无返回值，带线程名）
     *
     * @param backgroundTask 后台任务（在后台线程执行）
     * @param onSuccess      成功回调（在EDT线程执行）
     * @param onError        错误回调（在EDT线程执行）
     * @param threadName     线程名称
     */
    public static void execute(
            Runnable backgroundTask,
            Runnable onSuccess,
            Consumer<Exception> onError,
            String threadName
    ) {
        new Thread(() -> {
            try {
                backgroundTask.run();
                if (onSuccess != null) {
                    SwingUtilities.invokeLater(onSuccess);
                }
            } catch (Exception e) {
                log.error(ERROR_MESSAGE, e);
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                }
            }
        }, threadName).start();
    }

    /**
     * 执行异步任务（有返回值）
     *
     * @param backgroundTask 后台任务（在后台线程执行）
     * @param onSuccess      成功回调（在EDT线程执行）
     * @param onError        错误回调（在EDT线程执行）
     * @param <T>            返回值类型
     */
    public static <T> void execute(
            Callable<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Exception> onError
    ) {
        execute(backgroundTask, onSuccess, onError, DEFAULT_THREAD_NAME);
    }

    /**
     * 执行异步任务（有返回值，带线程名）
     *
     * @param backgroundTask 后台任务（在后台线程执行）
     * @param onSuccess      成功回调（在EDT线程执行）
     * @param onError        错误回调（在EDT线程执行）
     * @param threadName     线程名称
     * @param <T>            返回值类型
     */
    public static <T> void execute(
            Callable<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Exception> onError,
            String threadName
    ) {
        new Thread(() -> {
            try {
                T result = backgroundTask.call();
                if (onSuccess != null) {
                    SwingUtilities.invokeLater(() -> onSuccess.accept(result));
                }
            } catch (Exception e) {
                log.error(ERROR_MESSAGE, e);
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                }
            }
        }, threadName).start();
    }

    /**
     * 使用线程池执行异步任务（无返回值）
     *
     * @param backgroundTask 后台任务
     * @param onSuccess      成功回调
     * @param onError        错误回调
     */
    public static void executeWithPool(
            Runnable backgroundTask,
            Runnable onSuccess,
            Consumer<Exception> onError
    ) {
        DEFAULT_EXECUTOR.execute(() -> {
            try {
                backgroundTask.run();
                if (onSuccess != null) {
                    SwingUtilities.invokeLater(onSuccess);
                }
            } catch (Exception e) {
                log.error("Error executing async task in pool", e);
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                }
            }
        });
    }

    /**
     * 使用线程池执行异步任务（有返回值）
     *
     * @param backgroundTask 后台任务
     * @param onSuccess      成功回调
     * @param onError        错误回调
     * @param <T>            返回值类型
     * @return Future对象，可用于取消任务或获取结果
     */
    public static <T> Future<T> executeWithPool(
            Callable<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Exception> onError
    ) {
        return DEFAULT_EXECUTOR.submit(() -> {
            try {
                T result = backgroundTask.call();
                if (onSuccess != null) {
                    SwingUtilities.invokeLater(() -> onSuccess.accept(result));
                }
                return result;
            } catch (Exception e) {
                log.error("Error executing async task in pool", e);
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                }
                throw e;
            }
        });
    }

    /**
     * 创建异步任务构建器
     *
     * @param <T> 返回值类型
     * @return 构建器实例
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * 关闭默认线程池（应用退出时调用）
     */
    public static void shutdown() {
        DEFAULT_EXECUTOR.shutdown();
        try {
            if (!DEFAULT_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                DEFAULT_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            DEFAULT_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 异步任务构建器
     * <p>
     * 提供更灵活的方式构建异步任务
     * </p>
     *
     * @param <T> 返回值类型
     */
    public static class Builder<T> {
        private Callable<T> backgroundTask;
        private Consumer<T> onSuccess;
        private Consumer<Exception> onError;
        private Runnable onFinally;
        private String threadName = "AsyncTask";
        private boolean useThreadPool = false;

        /**
         * 设置后台任务
         */
        public Builder<T> backgroundTask(Callable<T> task) {
            this.backgroundTask = task;
            return this;
        }

        /**
         * 设置后台任务（无返回值）
         */
        public Builder<T> backgroundTask(Runnable task) {
            this.backgroundTask = () -> {
                task.run();
                return null;
            };
            return this;
        }

        /**
         * 设置成功回调
         */
        public Builder<T> onSuccess(Consumer<T> callback) {
            this.onSuccess = callback;
            return this;
        }

        /**
         * 设置成功回调（无参数）
         */
        public Builder<T> onSuccess(Runnable callback) {
            this.onSuccess = result -> callback.run();
            return this;
        }

        /**
         * 设置错误回调
         */
        public Builder<T> onError(Consumer<Exception> callback) {
            this.onError = callback;
            return this;
        }

        /**
         * 设置最终回调（无论成功或失败都会执行）
         */
        public Builder<T> onFinally(Runnable callback) {
            this.onFinally = callback;
            return this;
        }

        /**
         * 设置线程名称
         */
        public Builder<T> threadName(String name) {
            this.threadName = name;
            return this;
        }

        /**
         * 使用线程池执行
         */
        public Builder<T> useThreadPool() {
            this.useThreadPool = true;
            return this;
        }

        /**
         * 执行任务
         */
        public void execute() {
            if (backgroundTask == null) {
                throw new IllegalStateException("Background task is required");
            }

            if (useThreadPool) {
                DEFAULT_EXECUTOR.execute(() -> executeTask());
            } else {
                new Thread(this::executeTask, threadName).start();
            }
        }

        /**
         * 执行任务并返回Future（仅在使用线程池时有效）
         */
        public Future<T> submit() {
            if (backgroundTask == null) {
                throw new IllegalStateException("Background task is required");
            }
            return DEFAULT_EXECUTOR.submit(this::executeTaskWithResult);
        }

        private void executeTask() {
            try {
                T result = backgroundTask.call();
                if (onSuccess != null) {
                    SwingUtilities.invokeLater(() -> onSuccess.accept(result));
                }
            } catch (Exception e) {
                log.error(ERROR_MESSAGE, e);
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                }
            } finally {
                if (onFinally != null) {
                    SwingUtilities.invokeLater(onFinally);
                }
            }
        }

        private T executeTaskWithResult() throws Exception {
            try {
                T result = backgroundTask.call();
                if (onSuccess != null) {
                    SwingUtilities.invokeLater(() -> onSuccess.accept(result));
                }
                return result;
            } catch (Exception e) {
                log.error(ERROR_MESSAGE, e);
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                }
                throw e;
            } finally {
                if (onFinally != null) {
                    SwingUtilities.invokeLater(onFinally);
                }
            }
        }
    }
}

