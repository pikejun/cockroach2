package com.zhangyingwei.cockroach2.core.executor;

import com.zhangyingwei.cockroach2.common.generators.ICGenerator;
import com.zhangyingwei.cockroach2.common.generators.ICMapGenerator;
import com.zhangyingwei.cockroach2.common.generators.ICStringGenerator;
import com.zhangyingwei.cockroach2.core.config.CockroachConfig;
import com.zhangyingwei.cockroach2.core.http.CockroachHttpClient;
import com.zhangyingwei.cockroach2.core.queue.QueueHandler;
import com.zhangyingwei.cockroach2.http.proxy.ProxyInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zhangyw
 * @date: 2018/12/12
 * @desc:
 */
@Slf4j
public class ExecuterManager {
    private CockroachConfig config;
    private ExecutorService service = Executors.newCachedThreadPool();
    private List<TaskExecotor> executorList = new ArrayList<>();

    public ExecuterManager(CockroachConfig config) {
        this.config = config;
    }

    public void start(QueueHandler queue) throws IllegalAccessException, InstantiationException {
        int numThread = this.config.getNumThread();
        ICStringGenerator cookieGenerator = this.config.getCookieGeneratorClass() == null ? null : this.config.getCookieGeneratorClass().newInstance();
        ICMapGenerator headerGenerator = this.config.getHeaderGeneratorClass() == null ? null : this.config.getHeaderGeneratorClass().newInstance();
        for (int i = 0; i < numThread; i++) {
            TaskExecotor execotor = new TaskExecotor(
                    queue,
                    new CockroachHttpClient(
                            this.config.getHttpClientClass().newInstance(), cookieGenerator, headerGenerator
                    ),
                    this.createProxy(),
                    this.config.getStoreClass().newInstance(),
                    this.config.getThreadSleep()
            );
            this.service.execute(execotor);
            executorList.add(execotor);
        }
        // Monitor thread
        new Thread(() -> {
            Thread.currentThread().setName("TEMointor");
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(10);
                    ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) this.service;
                    int queueSize = poolExecutor.getQueue().size();
                    int activeCount = poolExecutor.getActiveCount();
                    long completedTaskCount = poolExecutor.getCompletedTaskCount();
                    long taskCount = poolExecutor.getTaskCount();
                    int runnableExecutor = executorList.stream().map(executor -> executor.getStatus()).filter(state -> Thread.State.RUNNABLE.equals(state)).collect(Collectors.toList()).size();
                    if (runnableExecutor == 0) {
                        log.info(
                                "queue size: {}, wait size:{},active size:{},completed size:{},task count:{}",
                                queue.size(),
                                queueSize,
                                activeCount,
                                completedTaskCount,
                                taskCount
                        );
                        this.service.execute(new TmpTaskExecotor(
                                queue,
                                new CockroachHttpClient(
                                        this.config.getHttpClientClass().newInstance(), cookieGenerator, headerGenerator
                                ),
                                this.createProxy(),
                                this.config.getStoreClass().newInstance(),
                                this.config.getThreadSleep()
                        ));
                        log.info("submit one tmpexecutor!");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private ICGenerator<ProxyInfo> createProxy() throws IllegalAccessException, InstantiationException {
        if (this.config.getProxyGeneratorClass() != null) {
            return this.config.getProxyGeneratorClass().newInstance();
        }
        return null;
    }

}