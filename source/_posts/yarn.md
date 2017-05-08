---
title: YARN基本架构
---

###  YARN基本架构
YARN是Hadoop 2.0中的资源管理系统，它的基本设计思想是将MRv1中的JobTracker拆分成了两个独立的服务：
  - 一个全局的资源管理器ResourceManager
  - 每个应用程序特有的ApplicationMaster。

  其中ResourceManager负责整个系统的资源管理和分配，而ApplicationMaster负责单个应用程序的管理。

YARN 总体上仍然是Master/Slave结构，在整个资源管理框架中，ResourceManager为Master，NodeManager为 Slave，ResourceManager负责对各个NodeManager上的资源进行统一管理和调度。当用户提交一个应用程序时，需要提供一个用以 跟踪和管理这个程序的ApplicationMaster，它负责向ResourceManager申请资源，并要求NodeManger启动可以占用一 定资源的任务。由于不同的ApplicationMaster被分布到不同的节点上，因此它们之间不会相互影响。
![yarn_architecture](http://hadoop.apache.org/docs/r3.0.0-alpha2/hadoop-yarn/hadoop-yarn-site/yarn_architecture.gif)

#### 1.ResourceManager(RM)
RM是一个全局的资源管理器，负责整个系统的资源管理和分配。它主要由两个组件构成：调度器（Scheduler）和应用程序管理器（Applications Manager，AM）。

(1)：调度器

调度器根据容量、队列等限制条件（如每个队列分配一定的资源，最多执行一定数量的作业等），将系统中的资源分配给各个正在运行的应用程序。需要注意的是，该调度器是一个“纯调度器”，它不再从事任何与具体应用程序相关的工作，比如不负责监控或者跟踪应用的执行状态等，也不负责重新启动因应用执 行失败或者硬件故障而产生的失败任务，这些均交由应用程序相关的ApplicationMaster完成。调度器仅根据各个应用程序的资源需求进行资源分 配，而资源分配单位用一个抽象概念“资源容器”（Resource Container，简称Container）表示，Container是一个动态资源分配单位，它将内存、 CPU、磁盘、网络等资源封装在一起，从而限定每个任务使用的资源量。此外，该调度器是一个可插拔的组件，用户可根据自己的需要设计新的调度器，YARN 提供了多种直接可用的调度器，比如Fair Scheduler和Capacity Scheduler等。

（2）:应用程序管理器

应用程序管理器负责管理整个系统中所有应用程序，包括应用程序提交、与调度器协商资源以启动ApplicationMaster、监控ApplicationMaster运行状态并在失败时重新启动它等。

#### 2.ApplicationMaster(AM)
用户提交的每个应用程序均包含1个AM，主要功能包括：

与RM调度器协商以获取资源（用Container表示）；

将得到的任务进一步分配给内部的任务；

与NM通信以启动/停止任务；

监控所有任务运行状态，并在任务运行失败时重新为任务申请资源以重启任务。

#### 3.NodeManager(NM)
NM是每个节点上的资源和任务管理器，一方面，它会定时地向RM汇报本节点上的资源使用情况和各个Container的运行状态；另一方面，它接收并处理来自AM的Container启动/停止等各种请求。

#### 4.Container
Container 是YARN中的资源抽象，它封装了某个节点上的多维度资源，如内存、CPU、磁盘、网络等，当AM向RM申请资源时，RM为AM返回的资源便是用 Container表示的。YARN会为每个任务分配一个Container，且该任务只能使用该Container中描述的资源。
目前，YARN仅支持CPU和内存两种资源，且使用了轻量级资源隔离机制Cgroups进行资源隔离。

### YARN工作流程
当用户向YARN中提交一个应用程序后，YARN将分两个阶段运行该应用程序：
第一个阶段是启动ApplicationMaster；
第二个阶段是由ApplicationMaster创建应用程序，为它申请资源，并监控它的整个运行过程，直到运行完成。

YARN的工作流程分为以下几个步骤：
- 步骤1：　用户向YARN中提交应用程序，其中包括ApplicationMaster程序、启动ApplicationMaster的命令、用户程序等。
- 步骤2：　ResourceManager为该应用程序分配第一个Container，并与对应的NodeManager通信，要求它在这个Container中启动应用程序的ApplicationMaster。
- 步骤3：　ApplicationMaster首先向ResourceManager注册，这样用户可以直接通过ResourceManager查看应用程序的运行状态，然后它将为各个任务申请资源，并监控它的运行状态，直到运行结束，即重复步骤4~7。
- 步骤4：　ApplicationMaster采用轮询的方式通过RPC协议向ResourceManager申请和领取资源。
- 步骤5：　一旦ApplicationMaster申请到资源后，便与对应的NodeManager通信，要求它启动任务。
- 步骤6：　NodeManager为任务设置好运行环境（包括环境变量、JAR包、二进制程序等）后，将任务启动命令写到一个脚本中，并通过运行该脚本启动任务。
- 步骤7：　各个任务通过某个RPC协议向ApplicationMaster汇报自己的状态和进度，以让ApplicationMaster随时掌握各个任务的运行状态，从而可以在任务失败时重新启动任务。在应用程序运行过程中，用户可随时通过RPC向ApplicationMaster查询应用程序的当前运行状态。
- 步骤8：　应用程序运行完成后，ApplicationMaster向ResourceManager注销并关闭自己。

### Hadoop: Writing YARN Applications
see http://hadoop.apache.org/docs/r3.0.0-alpha2/hadoop-yarn/hadoop-yarn-site/WritingYarnApplications.html

#### 1. 文件格式化与启动namenode&DataNode
```
$ bin/hdfs namenode -format

$ sbin/start-dfs.sh

```
#### 2. 启动RM&NM
```
$ sbin/start-yarn.sh

ResourceManager - http://localhost:8088/
```

#### 3. 例子：
包含了实现一个application的三个要求:
- 客户端和RM （Client.Java）
  - 客户端提交application
- AM和RM （ApplicationMaster.java）
  - 注册AM，申请分配container
- AM和NM （ApplicationMaster.java）
  - 启动container

执行命令：
```
hadoop jar hadoop-yarn-applications-distributedshell-3.0.0-alpha2.jar org.apache.hadoop.yarn.applications.distributedshell.Client -jar hadoop-yarn-applications-distributedshell-3.0.0-alpha2.jar -shell_command '/bin/date'
```
启动10个container，每个都执行`date`命令
执行代码流程:
1. 客户端通过org.apache.hadoop.yarn.applications.distributedshell.Client提交application到RM，需提供ApplicationSubmissionContext
2. org.apache.hadoop.yarn.applications.distributedshell.ApplicationMaster提交containers请求，执行用户提交的命令ContainerLaunchContext.commands

客户端(Client.java):
1. YarnClient.getNewApplication
2. 填充ApplicationSubmissionContext,ContainerLaunchContext（启动AM的Container）​
3. YarnClient.submitApplication​
4. 每隔一段时间调用YarnClient.getApplicationReport获得Application Status

```
  // 创建AM的上下文信息  
  ContainerLaunchContext amContainer = Records.newRecord(ContainerLaunchContext.class);  
  // 设置本地资源，AppMaster.jar包，log4j.properties  
  amContainer.setLocalResources(localResources);  
  // 环境变量,shell脚本在hdfs的地址, CLASSPATH  
  amContainer.setEnvironment(env);  
  // 设置启动AM的命令和参数  
  Vector<CharSequence> vargs = new Vector<CharSequence>(30);  
  vargs.add("${JAVA_HOME}" + "/bin/java");  
  vargs.add("-Xmx" + amMemory + "m");  
  // AM主类  
  vargs.add("org.apache.hadoop.yarn.applications.distributedshell.ApplicationMaster?");  
  vargs.add("--container_memory " + String.valueOf(containerMemory));  
  vargs.add("--num_containers " + String.valueOf(numContainers));  
  vargs.add("--priority " + String.valueOf(shellCmdPriority));  
  if (!shellCommand.isEmpty()) {  
  vargs.add("--shell_command " + shellCommand + "");  
  }  
  if (!shellArgs.isEmpty()) {  
  vargs.add("--shell_args " + shellArgs + "");  
  }  
  for (Map.Entry<String, String> entry : shellEnv.entrySet()) {  
  vargs.add("--shell_env " + entry.getKey() + "=" + entry.getValue());  
  }  
  vargs.add("1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stdout");  
  vargs.add("2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stderr");  

  amContainer.setCommands(commands);  
  // 设置Resource需求，目前只设置memory  
  capability.setMemory(amMemory);  
  amContainer.setResource(capability);  
  appContext.setAMContainerSpec(amContainer);  
  // 提交application到RM  
  super.submitApplication(appContext);
```
ApplicationMaster(ApplicationMaster.java​)
1. AMRMClient.registerApplicationMaster​​
2. 提供ContainerRequest到AMRMClient.addContainerRequest​
3. 通过AMRMClient.allocate获得container
4. container放入新建的LaunchContainerRunnable线程内执行
5. 创建ContainerLaunchContext​，设置localResource，shellcommand, shellArgs等​​container启动信息
6. ContainerManager.startContainer(startReq)​​
7. 下次RPC call后得到的Response信息，AMResponse.getCompletedContainersStatuses​​
8. AMRMClient.unregisterApplicationMaster​​

```
  // 新建AMRMClient，2.1beta版本实现了异步AMRMClient，这里还是同步的方式  
  resourceManager = new AMRMClientImpl(appAttemptID);  
  resourceManager.init(conf);  
  resourceManager.start();  
  // 向RM注册自己  
  RegisterApplicationMasterResponse response = resourceManager  
    .registerApplicationMaster(appMasterHostname, appMasterRpcPort,  
        appMasterTrackingUrl);  
  while (numCompletedContainers.get() < numTotalContainers && !appDone) {  
  // 封装Container请求，设置Resource需求，这边只设置了memory  
  ContainerRequest containerAsk = setupContainerAskForRM(askCount);  
  resourceManager.addContainerRequest(containerAsk);  

  // Send the request to RM  
  LOG.info("Asking RM for containers" + ", askCount=" + askCount);  
  AMResponse amResp = sendContainerAskToRM();  

  // Retrieve list of allocated containers from the response  
  List<Container> allocatedContainers = amResp.getAllocatedContainers();  
  for (Container allocatedContainer : allocatedContainers) {  
      //新建一个线程来提交container启动请求，这样主线程就不会被block住了  
      LaunchContainerRunnable runnableLaunchContainer = new LaunchContainerRunnable(  
        allocatedContainer);  
      Thread launchThread = new Thread(runnableLaunchContainer);  
      launchThreads.add(launchThread);  
      launchThread.start();  
  }  
  List<ContainerStatus> completedContainers = amResp.getCompletedContainersStatuses();  
  }  
  // 向RM注销自己  
  resourceManager.unregisterApplicationMaster(appStatus, appMessage, null);  
```
