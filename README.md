# 开发手记

MyProcess 流程图的配置:

1. 四种角色组 以及用的户定义

在 activiti-explorer 中管理条目中, 创建用户和组, 四种不同的用户以及角色:

    1. 代理  - agent
    1. 咨询顾问 - counsellor
    2. 入学顾问 - admission_counselor
    3. 客服 - custom_service
    4. 注册专员 - ra_specialist


密码统统设置为 8 个 1。

手动制造用户:

代理: agent2, agent1
咨询顾问: coun1, coun2
入学顾问: adcoun1, adcoun2
客服: custom_service1, custom_service2
注册专员: ra_spec1, ra_spec2

11111111

邮箱:

xiajian@qq.com
wanghe@qq.com
xiajian@tophold.com
wanghe@tophold.com
ra_spec1@yundianjia.com


信息创建成功:

![](http://ww4.sinaimg.cn/large/006tNc79gw1fasn3axemzj30ju0fwta4.jpg)

思考:

如何通过 API 或者数据的方式, 来批量的插入数据, 线上可以先用测试数据库。

需要编写 sql 脚本(mysql 版):

尝试存储过程 - 失败:

```
use activiti;

drop procedure if exists looppc;
create procedure looppc()
BEGIN
    DECLARE i INT;
    SET i = 1;

    REPEAT
        insert into ACT_ID_USER (ID_, FIRST_, LAST_, EMAIL_, PWD_) values (CONCAT('ra_spec', i),'注册专员', i, '1540469793@qq.com','11111111')
        SET i = i + 1;
        UNTIL i >= 10
    END REPEAT;
END

call looppc();
```


手写 sql 语句, 进入数据后, 直接使用 source 语句就可以了, 但是手写比较的重复以及冗余。 所以, 需要使用脚本化的处理。

```
insert into ACT_ID_USER (ID_, FIRST_, LAST_, EMAIL_, PWD_) values
        ('agent1','代理','1','1540469793@qq.com','11111111'),
        ('agent2','代理','2','1540469793@qq.com','11111111'),
        ('agent3','代理','3','1540469793@qq.com','11111111'),
        ('agent4','代理','4','1540469793@qq.com','11111111'),
        ('agent5','代理','5','1540469793@qq.com','11111111'),
        ('agent6','代理','6','1540469793@qq.com','11111111'),
        ('agent7','代理','7','1540469793@qq.com','11111111'),
        ('agent8','代理','8','1540469793@qq.com','11111111'),
        ('agent9','代理','9','1540469793@qq.com','11111111'),
        ('agent10','代理','10','1540469793@qq.com','11111111'),
        ('agent11','代理','11','1540469793@qq.com','11111111'),
```

脚本处理, 批量处理相同作用的用户:

```ruby
require 'mysql2'

client = Mysql2::Client.new(
  :host => '127.0.0.1', # 主机
  :username => 'root', # 用户名
  :password => '', # 密码
  :database => 'activiti', # 数据库
  :encoding => 'utf8' # 编码
)

roles = [
    {role_name: '代理',  name: 'agent' },
    {role_name: '咨询顾问',  name: 'counsellor' },
    {role_name: '入学顾问',  name: 'admission_counselor' },
    {role_name: '客服',  name: 'custom_service' },
    {role_name: '注册专员',  name: 'ra_specialist' }
]

roles.each do |role|
  3.upto(10).each do |i|
    begin
      user_name = "#{role[:name]}#{i}"
      client.query "insert into ACT_ID_USER (ID_, FIRST_, LAST_, EMAIL_, PWD_, REV_) values ('#{user_name}', '#{role[:role_name]}','#{i}','1540469793@qq.com','11111111', '1')"

      # 有外键索引的表, 不太好加
      # client.query "insert into ACT_ID_MEMBERSHIP (USER_ID_, GROUP_ID_) values ('#{user_name}', '#{role[:role_name]}')"
    rescue => e
      puts e
      next
    end
  end

  client.query "insert into ACT_ID_GROUP (ID_, REV_, NAME_, TYPE_) values ('#{role[:name]}', '1', '#{role[:role_name]}', 'assignment')"
end
```

初步测试, 将流程图上传上去,然后执行可行的。

2. 定义一个具体的任务  - 比如, 初始启动的任务, 设置动态表单

    1. 设置动态的表单 - 看具体的需求
    2. 如何关联到已经模型中

动态表单:

    1. 在输入比较简单时,  可以使用; 输入复杂时, 可以使用外置表单. form 也算是资源之一。

如果要定义外部任务, 以考虑 `shell 任务`, 然后利用 rails 这样的 shell 脚本来运行 ruby 代码。

shell 脚本的参数:

```
属性	是否必须	类型	描述	默认值
command	是	String	执行的shell命令
arg0-5	否	String	参数0至5
wait	否	true/false	是否需要等待到shell进程结束	true
redirectError	否	true/false	把标准错误打印到标准流中	false
cleanEnv	否	true/false	shell进行不继承当前环境	false
outputVariable	否	String	保存输出的变量名	不会记录输出结果
errorCodeVariable	否	String	包含结果错误代码的变量名	不会注册错误级别
directory	否	String	shell进程的默认目录	当前目录
```

例子, shell 脚本:

```
<serviceTask id="shellEcho" activiti:type="shell" >
  <extensionElements>
    <activiti:field name="command" stringValue="cmd" />
    <activiti:field name="arg1" stringValue="/c" />
    <activiti:field name="arg2" stringValue="echo" />
    <activiti:field name="arg3" stringValue="EchoTest" />
    <activiti:field name="wait" stringValue="true" />
    <activiti:field name="outputVariable" stringValue="resultVar" />
  </extensionElements>
</serviceTask>
```


> 需要动态设置表单的必要性很小, 动态表单可以通过 idea 中的数据来设置。

当前, 需要处理就是: 在复杂的流程图, 如何通过代码来执行流程? 遍历查找代码。

3. 设置跳转条件

通过在条件判断的 sequence flow 上设置的, 变量是 通过 `execution.setVariable` 方法设置。

```
task.complete id, variables
```

如何在 jruby 中使用, 感觉语法上回更加美好一点。

4. 编写测试用例

编写运行的用例, 设置全流程的测试。

5. 如何添加异常的处理事件

先学习了解 如何异常如何处理。


打包部署的文件中:

    1. bpmn 或者 bpmn.xml 的内容
    2. bpmn 的图片描述


问题: 没有找到将 bpmn 文件 生成图片的方式。

java 项目的 `.idea/` 目录是不能删除掉的, 删除掉就挂了, 感谢万能的 git ,保护了我。












## 讨论

工作流,仅仅用作 为流程的流转.

指定用户 以及角色的 工作流程设置:

1. 启动流程时, 动态设置角色
2. 获取具体的任务
3. 动态指定候选人
4. 提交具体任务
5. 获取历史数据

## 思考

Idea 到底缓存的是啥 ?? 感觉连一些状态都缓存下来了。 还是什么