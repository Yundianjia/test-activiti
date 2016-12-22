package org.crazyit.activiti;

import org.activiti.engine.*;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;


/**
 * 第一个流程运行类
 *
 * @author yangenxiong
 */
public class First {

    // 创建流程引擎 - 流程引擎有七个小矮人接口
    private static ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();

    public static void main(String[] args) {
        testFirstflow();
    }

    /**
     * 这是一个类的静态方法的定义
     */
    public static void testFirstflow() {
        // 得到流程存储服务组件
        RepositoryService repositoryService = engine.getRepositoryService();
        // 得到运行时服务组件
        RuntimeService runtimeService = engine.getRuntimeService();
        // 获取流程任务组件
        TaskService taskService = engine.getTaskService();
        // 部署流程文件
        repositoryService.createDeployment()
                .addClasspathResource("bpmn/First.bpmn").deploy();
        // 启动流程
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process1");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        System.out.println("第一个任务完成前，当前任务名称：" + task.getName());

        // 完成第一个任务 - complete 触发任务完成
        taskService.complete(task.getId());

        // 查询第二个任务
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        System.out.println("第二个任务完成前，当前任务名称：" + task.getName());
        // 完成第二个任务（流程结束）
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        System.out.println("流程结束后，查找任务：" + task);
    }
}
