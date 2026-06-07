package chem_data_platform.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async task executor configuration
 * Used to run file analysis tasks in the background by calling the Qwen API
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Configure the async executor
     * - Core pool size: 2
     * - Maximum pool size: 5
     * - Queue capacity: 100
     * - Thread name prefix: qwen-analysis-
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);                      // Core thread count
        executor.setMaxPoolSize(5);                       // Maximum thread count
        executor.setQueueCapacity(100);                   // Task queue size
        executor.setThreadNamePrefix("qwen-analysis-");   // Thread name prefix
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
