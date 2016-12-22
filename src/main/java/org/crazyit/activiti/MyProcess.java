package org.crazyit.activiti;

import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xiajian on 16/12/19.
 */
public class MyProcess {
    // 创建流程引擎 - 流程引擎有七个小矮人接口
    private static ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();

    public static void main(String[] args) {
        testMyProcessNormalflow();
    }

    /**
     * 测试留学申请的正常的流程, 没有涉及添加子流程, 没有触发异常事件, 一切都是正常的流程
     * <p>
     * 历时许久, 1天写了 130 行 java 代码总于整好, 为此, 纪念一下。
     *
     * claim 与 setAssignee 方法类似, complete 与 signal 方法也有同工之处.
     *
     */
    public static void testMyProcessNormalflow() {
        Map<String, Object> services = getTaskService("bpmn/MyProcess.bpmn", "myProcess");
        // 获取任务列表
        TaskService taskService = (TaskService) services.get("taskService");
        ProcessInstance processInstance = (ProcessInstance) services.get("processInstance");

        String agent1 = "agent1";   // 代理
        String coun1 = "coun1";     // 咨询顾问
        String adcoun1 = "adcoun1"; // 入学顾问
        String ra_spec1 = "ra_spec1";  // 注册专员
        String custom_service1 = "custom_service1"; // 客服
        Map<String, Object> vars = new HashMap<String, Object>();

        System.out.println("留学申请流程开始。。。。。。。。。。");

        long agent_count = taskService.createTaskQuery().taskCandidateGroup("agent").count();
        long counsellor_count = taskService.createTaskQuery().taskCandidateGroup("counsellor").count();
        long custom_service_count = taskService.createTaskQuery().taskCandidateGroup("custom_service").count();
        long admission_counselor_count = taskService.createTaskQuery().taskCandidateGroup("admission_counselor").count();
        long ra_specialist_count = taskService.createTaskQuery().taskCandidateGroup("ra_specialist").count();

        System.out.println("代理的数目: " + agent_count);
        System.out.println("咨询顾问的数目: " + counsellor_count);
        System.out.println("客服任务的数目: " + custom_service_count);
        System.out.println("入学顾问的数目: " + admission_counselor_count);
        System.out.println("注册专员的数目: " + ra_specialist_count);

        // 列表就不能使用使用
        List<Task> taskList = taskService.createTaskQuery().taskCandidateGroup("agent").processInstanceId(processInstance.getId()).list();

        for (Task t : taskList) {
            System.out.println(t.getName() + " candidate group is agent, and assignee is " + t.getAssignee());
        }

        // Task agent1Task = taskService.createTaskQuery().taskAssignee(agent1).singleResult();
        // System.out.println("agent1 的第一个任务: " + agent1Task.getName());

        // 第一个任务: 方案审核 - 代理
        // 遇到一个问题: 启动的流程实例太多, 执行报错。
        Task task = taskService.createTaskQuery().taskCandidateGroup("agent").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task.getName());
        taskService.setAssignee(task.getId(), agent1);
        taskService.complete(task.getId());

        // 并行网关 1:  如何处理条件部分, 并行任务不需要设置特定的代码处理

        // 分支 2-1-1 缴纳申请费 - 代理
        Task task2 = taskService.createTaskQuery().taskCandidateGroup("agent").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task2.getName());
        taskService.setAssignee(task2.getId(), agent1);

        // 分支 2-2-1 材料完整性审核 - 咨询顾问
        Task task2_1 = taskService.createTaskQuery().taskCandidateGroup("counsellor").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task2_1.getName());
        taskService.setAssignee(task2_1.getId(), coun1);
        taskService.complete(task2_1.getId());

        // 分支 2-2-2 材料内容审核 - 客服
        Task task2_2 = taskService.createTaskQuery().taskCandidateGroup("custom_service").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task2_2.getName());
        taskService.setAssignee(task2_2.getId(), custom_service1);

        // 节点 - 并行网关,  需要分开的同事处理结束任务
        taskService.complete(task2_2.getId());
        taskService.complete(task2.getId());

        // 任务: 录入申请信息 - 客服
        task = taskService.createTaskQuery().taskCandidateGroup("custom_service").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task.getName());
        taskService.setAssignee(task.getId(), custom_service1);
        taskService.complete(task.getId());

        // 任务: 录入结果 - 入学顾问
        task = taskService.createTaskQuery().taskCandidateGroup("admission_counselor").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task.getName());
        taskService.setAssignee(task.getId(), adcoun1);
        taskService.complete(task.getId());

        // 任务: 结果分析 - 咨询顾问
        task = taskService.createTaskQuery().taskCandidateGroup("counsellor").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task.getName());
        taskService.setAssignee(task.getId(), coun1);
        taskService.complete(task.getId());

        // 任务: 确认付费方案 - 咨询顾问
        task = taskService.createTaskQuery().taskCandidateGroup("counsellor").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task.getName());
        taskService.setAssignee(task.getId(), coun1);
        taskService.complete(task.getId());

        // 任务: 支付学费 - 代理
        task = taskService.createTaskQuery().taskCandidateGroup("agent").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task.getName());
        taskService.setAssignee(task.getId(), agent1);
        taskService.complete(task.getId());

        // 任务: COE 代理 - 代理
        task = taskService.createTaskQuery().taskCandidateGroup("agent").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task.getName());
        taskService.setAssignee(task.getId(), agent1);
        taskService.complete(task.getId());

        // 任务: COE 材料审核 - 客服
        task = taskService.createTaskQuery().taskCandidateGroup("custom_service").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task.getName());
        taskService.setAssignee(task.getId(), custom_service1);
        taskService.complete(task.getId());

        // 任务: COE 申请递交 - 客服
        task = taskService.createTaskQuery().taskCandidateGroup("custom_service").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task.getName());
        taskService.setAssignee(task.getId(), custom_service1);
        taskService.complete(task.getId());

        // 任务: COE 结果录入 - 注册专员
        task = taskService.createTaskQuery().taskCandidateGroup("ra_specialist").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task.getName());
        taskService.setAssignee(task.getId(), ra_spec1);
        taskService.complete(task.getId());

        // 任务: 确认环节 - 咨询顾问
        task = taskService.createTaskQuery().taskCandidateGroup("counsellor").processInstanceId(processInstance.getId()).singleResult();
        System.out.println(getCurrentTime() + " 当前任务: " + task.getName());
        taskService.setAssignee(task.getId(), coun1);
        // 排他网关 2: 是通过 HashMap 的对象实现的, 这里直接让其通过, 让后流程开始。
        vars = new HashMap<String, Object>();
        vars.put("confirm", true);
        taskService.complete(task.getId(), vars);

        System.out.println("流程正常结束。。。。。。。。。。");
        // printHistoryMessage();
    }

    protected static Map<String, Object> getTaskService(String resource, String processKey) {
        Map<String, Object> map = new HashMap<String, Object>();

        // 得到流程存储服务组件
        RepositoryService repositoryService = engine.getRepositoryService();
        // 得到运行时服务组件
        RuntimeService runtimeService = engine.getRuntimeService();
        // 获取流程任务组件
        TaskService taskService = engine.getTaskService();
        // 部署流程文件
        repositoryService.createDeployment()
                .addClasspathResource(resource).deploy();
        // 启动流程
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey);

        // 查询为空且报错, 将异常 catch 起来。
        try {
            List<ProcessInstance> processInstanceList = runtimeService.createProcessInstanceQuery().list();
            List<Execution> executionList = runtimeService.createNativeExecutionQuery().list();
            System.out.println("processInstanceList 的长度: " + processInstanceList.size());
            System.out.println("executionList 的长度度: " + executionList.size());
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }

        map.put("taskService", taskService);
        map.put("processInstance", processInstance);

        return map;
    }

    /**
     * 测试异常流程的代码
     */
    public static void testMyProcessExpectionflow() {
        Map<String, Object> services = getTaskService("bpmn/MyProcessWithException.bpmn", "myProcess");
        // 获取任务列表
        TaskService taskService = (TaskService) services.get("taskService");
        ProcessInstance processInstance = (ProcessInstance) services.get("processInstance");
    }

    /**
     * 打印历史执行记录的输出, 看起来是全量的历史查询, 记录比较多,
     */
    protected static void printHistoryMessage() {
        System.out.println("查询历史数据开始。。。。。。。。。。");

        // 验证历史数据
        HistoryService historyService = engine.getHistoryService();
        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery().list();
        for (HistoricActivityInstance activity : list) {
            System.out.println(activity.getActivityName() + " = " + activity.getEndTime());
        }

        System.out.println("查询历史数据结束。。。。。。。。。。");
    }

    protected static String getCurrentTime() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String curdate = simpleDateFormat.format(date);

        return curdate;
    }
}
