package chem_data_platform.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务执行器配置
 * 用于后台执行文件分析任务（调用 Kimi 或讯飞星火 API）
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 配置异步执行器
     * - 核心线程池大小：2
     * - 最大线程数：5
     * - 队列容量：100
     * - 线程名前缀：kimi-analysis-
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);                      // 核心线程数
        executor.setMaxPoolSize(5);                       // 最大线程数
        executor.setQueueCapacity(100);                   // 任务队列大小
        executor.setThreadNamePrefix("kimi-analysis-");   // 线程名前缀
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
