package com.zhangyingwei.cockroach2.common;

import com.zhangyingwei.cockroach2.common.enmus.RequestType;
import com.zhangyingwei.cockroach2.common.exception.CockroachUrlNotValidException;
import com.zhangyingwei.cockroach2.common.utils.IdUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangyw
 * @date: 2018/12/11
 * @desc:
 */

@EqualsAndHashCode
@Slf4j
public class Task implements Comparable<Task> {
    @Getter
    private Long id = IdUtils.getId(Task.class.getName());
    @Setter @Getter
    private String group = Constants.TASK_GROUP_DEFAULT;
    @Getter @Setter
    private String url;
    @Getter
    @Setter
    private Map<String, String> params = new HashMap<>();
    @Setter
    private Object data;
    @Getter
    private RequestType requestType = RequestType.GET;
    @Getter
    private Statu statu = Statu.CREATE;


    /**
     * 任务重试次数
     */
    private Integer tryNum = Constants.TASK_RETRY;
    /**
     * 任务优先级
     */
    @Setter @Getter
    private Integer priority = Constants.TASK_PRIORITY;

    public Task(String url) {
        this.url = url;
    }

    public Task(String url, String group) {
        this.group = group;
        this.url = url;
    }

    public <T> T getData() {
        return (T) data;
    }

    public Task group(String group) {
        this.group = group;
        return this;
    }

    public Task tryNum(Integer num) {
        this.tryNum = num;
        return this;
    }

    public Task statu(Statu statu) {
        this.statu = statu;
        return this;
    }

    public Boolean needRetry() {
        return this.tryNum > 0;
    }

    public Task tryOne() {
        this.tryNum -= 1;
        if (this.tryNum < 0) {
            log.info("Task({}) retry is over",this.id);
            return null;
        }
        return this;
    }

    /**
     * 基于父 task 生成比之更高一级的优先级
     * @param fatherTask
     * @return
     */
    public Task higherPriorityBy(Task fatherTask) {
        this.priority = fatherTask.getPriority() + 1;
        return this;
    }

    /**
     * 基于父 task 生成比之更低一级的优先级
     * @param fatherTask
     * @return
     */
    public Task lowerPriorityBy(Task fatherTask) {
        this.priority = fatherTask.getPriority() - 1;
        return this;
    }


    @Override
    public int compareTo(Task other) {
        int priorityCompare = other.getPriority() - this.getPriority();
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        return (int)(other.id - this.id);
    }

    public Task doWith(RequestType type) {
        this.requestType = type;
        return this;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id:" + id +
                ", group:'" + group + '\'' +
                ", priority:'" + priority + '\'' +
                ", url:'" + url + '\'' +
                ", params:" + params +
                ", requestType:" + requestType +
                '}';
    }

    public enum Statu {
        CREATE,
        START,
        EXECUTE,
        STORE,
        VALID, FAILD, FINISH
    }
}
