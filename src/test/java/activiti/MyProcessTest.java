package activiti;


import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.FileInputStream;

import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/**
 * Created by xiajian on 16/12/19.
 * activitiRule 用来测试的类, 其中, 封装了包含该 engine 在内的许多接口
 * 是扩展子 junit.test.Rule 的类
 */
public class MyProcessTest {

    private String resource = "bpmn/MyProcess.bpmn";
    private String agent1 = "agent1";   // 代理
    private String coun1 = "coun1";     // 咨询顾问
    private String adcoun1 = "adcoun1"; // 入学顾问
    private String ra_spec1 = "ra_spec1";  // 注册专员
    private String custom_service1 = "custom_service1"; // 客服

    @Rule
    public ActivitiRule activitiRule = new ActivitiRule();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.out.println("In beofore setting user");
    }

    @Test
    @Deployment(resources = {"bpmn/MyProcess.bpmn"})
    public void testNormalFlow() throws Exception {
        RuntimeService runtimeService = activitiRule.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess");
        TaskService taskService = activitiRule.getTaskService();
        IdentityService identityService = activitiRule.getIdentityService();

        setUserData(identityService);

        assertNotNull(processInstance.getId());

        String agent1 = "agent1";   // 代理
        String coun1 = "coun1";     // 咨询顾问
        String adcoun1 = "adcoun1"; // 入学顾问
        String ra_spec1 = "ra_spec1";  // 注册专员
        String custom_service1 = "custom_service1"; // 客服
        Map<String, Object> vars = new HashMap<String, Object>();

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
    }


    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        System.out.println("流程正常结束。。。。。。。。。。");
    }

    protected static String getCurrentTime() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String curdate = simpleDateFormat.format(date);

        return curdate;
    }

    //创建用户方法
    protected static void createUser(IdentityService identityService, String id, String first, String last, String email, String passwd) {
        // 使用newUser方法创建User实例
        User user = identityService.newUser(id);
        // 设置用户的各个属性
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setPassword(passwd);
        // 使用saveUser方法保存用户
        identityService.saveUser(user);
    }

    // 将用户组数据保存到数据库中
    protected static void createGroup(IdentityService identityService, String id,
                                      String name, String type) {
        // 调用newGroup方法创建Group实例
        Group group = identityService.newGroup(id);
        group.setName(name);
        group.setType(type);
        identityService.saveGroup(group);
    }

    protected static void setUserData(IdentityService identityService) {
        Map user_groups = new HashMap();
        user_groups.put("agent", "代理");
        user_groups.put("counsellor", "咨询顾问");
        user_groups.put("custom_service", "客服");
        user_groups.put("admission_counselor", "入学顾问");
        user_groups.put("ra_specialist", "注册专员");

        Iterator entries = user_groups.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            System.out.println("key: " + key);
            System.out.println("value: " + value);

            createUser(identityService, key + "1", value, "1", "xiajian@qq.com", "11111111");
            createGroup(identityService, key, value, "assignment");

            identityService.createMembership(key + "1", key);
        }

        System.out.println("留学申请流程开始。。。。。。。。。。");
    }
}
