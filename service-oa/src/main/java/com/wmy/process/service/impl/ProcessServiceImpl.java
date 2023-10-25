package com.wmy.process.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmy.auth.service.SysUserService;
import com.wmy.model.process.ProcessRecord;
import com.wmy.model.process.ProcessTemplate;
import com.wmy.model.system.SysUser;
import com.wmy.process.mapper.ProcessMapper;
import com.wmy.process.service.ProcessRecordService;
import com.wmy.process.service.ProcessService;
import com.wmy.process.service.ProcessTemplateService;
import com.wmy.security.custom.LoginUserInfoHelper;
import com.wmy.vo.process.ApprovalVo;
import com.wmy.vo.process.ProcessFormVo;
import com.wmy.vo.process.ProcessQueryVo;
import com.wmy.vo.process.ProcessVo;
import com.wmy.model.process.Process;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class ProcessServiceImpl extends ServiceImpl<ProcessMapper, Process> implements ProcessService {

    @Autowired
    private ProcessMapper processMapper;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ProcessTemplateService processTemplateService;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private ProcessRecordService processRecordService;
    @Autowired
    private HistoryService historyService;

    /**
     * 审批管理列表
     * @param pageParam
     * @param processQueryVo
     * @return
     */
    @Override
    public IPage<ProcessVo> selectPage(Page<ProcessVo> pageParam, ProcessQueryVo processQueryVo) {
        IPage<ProcessVo> page = processMapper.selectPage(pageParam, processQueryVo);
        return page;
    }


    @Override
    public void deployByZip(String deployPath) {
        InputStream inputStream =
                this.getClass().getClassLoader().getResourceAsStream(deployPath);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        //部署
        Deployment deployment = repositoryService.createDeployment()
                .addZipInputStream(zipInputStream)
                .deploy();
    }

    @Override
    public void startUp(ProcessFormVo processFormVo) {
        //1 根据当前用户id获取用户信息
        SysUser sysUser = sysUserService.getById(LoginUserInfoHelper.getUserId());

        //2 根据审批模板id把模板信息查询出来
        ProcessTemplate processTemplate = processTemplateService.getById(processFormVo.getProcessTemplateId());

        //3 保存提交审批信息到业务表，oa_process
        Process process = new Process();
        BeanUtils.copyProperties(processFormVo, process);
        process.setStatus(1); //审批中
        String workNo = System.currentTimeMillis() + ""; //生成当前时间
        process.setProcessCode(workNo);
        process.setUserId(LoginUserInfoHelper.getUserId());
        process.setFormValues(processFormVo.getFormValues());
        process.setTitle(sysUser.getName() + "发起" + processTemplate.getName());
        processMapper.insert(process);

        //4 启动流程实例
        //4.1 流程定义key
        String processDefinitionKey = processTemplate.getProcessDefinitionKey();
        //4.2 业务key-od_process表的processId
        String businessKey = String.valueOf(process.getId());
        //4.3 流程参数 form表单json数据，转换为map
        String formValues = processFormVo.getFormValues();
        JSONObject jsonObject = JSON.parseObject(formValues);
        JSONObject formData = jsonObject.getJSONObject("formData");
        //遍历formData得到内容，封装map集合
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("data", map);
        //数据准备完成，开启流程实例
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey,
                businessKey, variables);

        //5 查询审批人
        // 审批人可能是多个
        List<Task> taskList = this.getCurrentTaskList(processInstance.getId());
        List<String> nameList = new ArrayList<>();
        for (Task task : taskList) {
            String assigneeName = task.getAssignee();
            SysUser user = sysUserService.getByUsername(assigneeName);
            String name = user.getName();
            nameList.add(name);
            //6 给审批人推送消息

        }
        process.setProcessInstanceId(processInstance.getId());
        process.setDescription("等待" + StringUtils.join(nameList.toArray(), ",") + "审批");
        //7 业务和流程进行关联 , 更新oa_process的上面没有添加的字段
        baseMapper.updateById(process);

        //记录操作审批信息记录
        processRecordService.record(process.getId(),1,"发起申请");

    }


    /**
     * 查询待处理任务列表
     * @param pageParam
     * @return
     */
    @Override
    public IPage<ProcessVo> findPending(Page<Process> pageParam) {
        //1.封装查询条件，根据当前登录的用户名称
        TaskQuery query = taskService
                .createTaskQuery()
                .taskAssignee(LoginUserInfoHelper.getUsername())
                .orderByTaskCreateTime()
                .desc();

        //2.调用方法分页条件查询，返回list集合（待办任务集合）
        //listPage()有两个参数（开始位置，结束位置）
        int begin = (int) ((pageParam.getCurrent()-1) * pageParam.getSize());
        int size = (int) pageParam.getSize();
        List<Task> taskList = query.listPage(begin, size);
        long totalCount = query.count();

        //3.封装发布会list集合数据到List<ProcessVo>里面
        //task=>process=>processVo
        List<ProcessVo> processVoList = new ArrayList<>();
        for (Task task : taskList) {
            //从task获取流程实例id
            String processInstanceId = task.getProcessInstanceId();
            //根据流程实例id获取实例对象
            ProcessInstance processInstance = runtimeService
                    .createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            if (processInstance == null) {
                continue;
            }
            //从流程实例对象获取业务key
            String businessKey = processInstance.getBusinessKey();
            if (businessKey == null) {
                continue;
            }
            //根据业务key获取Process对象
            long processId = Long.parseLong(businessKey);
            Process process = baseMapper.selectById(processId);
            //Process对象复制到ProcessVo
            ProcessVo processVo = new ProcessVo();
            BeanUtils.copyProperties(process, processVo);
            processVo.setTaskId(task.getId());
            processVoList.add(processVo);
        }

        //4.封装返回IPage对象
        IPage<ProcessVo> page = new Page<ProcessVo>(pageParam.getCurrent(), pageParam.getSize(), totalCount);
        page.setRecords(processVoList);
        return page;
    }

    /**
     * 查看审批详情
     * @param id
     * @return
     */
    @Override
    public Map<String, Object> show(Long id) {
        //1.根据流程id获取流程记录信息Process
        Process process = baseMapper.selectById(id);

        //2.根据流程id获取流程记录信息
        LambdaQueryWrapper<ProcessRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessRecord::getProcessId, id);
        List<ProcessRecord> processRecordList = processRecordService.list(wrapper);

        //3.根据模板id查询模板信息
        ProcessTemplate processTemplate = processTemplateService.getById(process.getProcessTemplateId());

        //4.判断当前用户是否有权限进行审批，且不能重复审批
        boolean isApprove = false;
        List<Task> taskList = this.getCurrentTaskList(process.getProcessInstanceId());
        for (Task task : taskList) {
            //判断审批人是否是当前用户
            if (task.getAssignee().equals(LoginUserInfoHelper.getUsername())) {
                isApprove = true;
            }
        }

        //5.封装查询数据，返回
        Map<String, Object> map = new HashMap<>();
        map.put("process", process);
        map.put("processRecordList", processRecordList);
        map.put("processTemplate", processTemplate);
        map.put("isApprove", isApprove);
        return map;
    }

    /**
     * 审批通过与驳回操作
     * @param approvalVo
     */
    @Override
    public void approve(ApprovalVo approvalVo) {
        //1.从approvalVo获取任务id，根据id获取流程变量
        String taskId = approvalVo.getTaskId();
        Map<String, Object> variables = taskService.getVariables(taskId);
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        }

        //2.判断审批状态值
        if (approvalVo.getStatus() == 1) {
            //2.1 状态值=1审批通过
            Map<String, Object> variable = new HashMap<>();
            taskService.complete(taskId, variable);
        } else {
            //2.2 状态值=-1驳回，流程直接结束
            this.endTask(taskId);
        }

        //3.记录审批相关过程信息oa_process_record
        String description = approvalVo.getStatus().intValue() == 1 ? "已通过" : "驳回";
        processRecordService.record(approvalVo.getProcessId(), approvalVo.getStatus(), description);

        //4.查询下一个审批人，更新流程表记录process
        Process process = baseMapper.selectById(approvalVo.getProcessId());
        //查询任务
        List<Task> taskList = this.getCurrentTaskList(process.getProcessInstanceId());
        if (!CollectionUtils.isEmpty(taskList)) {
            List<String> assignList = new ArrayList<>();
            for (Task task : taskList) {
                SysUser sysUser = sysUserService.getByUsername(task.getAssignee());
                assignList.add(sysUser.getName());
            }
            process.setDescription("等待" + StringUtils.join(assignList.toArray(), ",") + "审批");
            process.setStatus(1);
        } else {
            if(approvalVo.getStatus().intValue() == 1) {
                process.setDescription("审批完成（通过）");
                process.setStatus(2);
            } else {
                process.setDescription("审批完成（驳回）");
                process.setStatus(-1);
            }
        }
        baseMapper.updateById(process);
    }

    /**
     * 查询已处理列表
     * @param pageParam
     * @return
     */
    @Override
    public IPage<ProcessVo> findProcessed(Page<Process> pageParam) {
        /// 根据当前人的ID查询
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery().taskAssignee(LoginUserInfoHelper.getUsername()).finished().orderByTaskCreateTime().desc();
        List<HistoricTaskInstance> list = query.listPage((int) ((pageParam.getCurrent() - 1) * pageParam.getSize()), (int) pageParam.getSize());
        long totalCount = query.count();

        List<ProcessVo> processList = new ArrayList<>();
        for (HistoricTaskInstance item : list) {
            String processInstanceId = item.getProcessInstanceId();
            Process process = this.getOne(new LambdaQueryWrapper<Process>().eq(Process::getProcessInstanceId, processInstanceId));
            ProcessVo processVo = new ProcessVo();
            BeanUtils.copyProperties(process, processVo);
            processVo.setTaskId("0");
            processList.add(processVo);
        }
        IPage<ProcessVo> page = new Page<ProcessVo>(pageParam.getCurrent(), pageParam.getSize(), totalCount);
        page.setRecords(processList);
        return page;
    }

    @Override
    public IPage<ProcessVo> findStarted(Page<ProcessVo> pageParam) {
        ProcessQueryVo processQueryVo = new ProcessQueryVo();
        processQueryVo.setUserId(LoginUserInfoHelper.getUserId());
        IPage<ProcessVo> page = processMapper.selectPage(pageParam, processQueryVo);
        for (ProcessVo item : page.getRecords()) {
            item.setTaskId("0");
        }
        return page;
    }


    /**
     * 结束流程
     * @param taskId
     */
    private void endTask(String taskId) {
        //1.根据任务id获取任务对象 Task
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        //2.获取流程定义的模型
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());

        //3.获取结束流向节点
        List endEventList = bpmnModel.getMainProcess().findFlowElementsOfType(EndEvent.class);
        if(CollectionUtils.isEmpty(endEventList)) {
            return;
        }
        FlowNode endFlowNode = (FlowNode) endEventList.get(0);
        //4.当前流行节点
        FlowNode currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());

        //5.把下一个节点的流向断开，直接指向结束节点
        //  临时保存当前活动的原始方向
        List originalSequenceFlowList = new ArrayList<>();
        originalSequenceFlowList.addAll(currentFlowNode.getOutgoingFlows());
        //  清理活动方向
        currentFlowNode.getOutgoingFlows().clear();

        //  建立新方向
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(currentFlowNode);
        newSequenceFlow.setTargetFlowElement(endFlowNode);
        List newSequenceFlowList = new ArrayList<>();
        newSequenceFlowList.add(newSequenceFlow);
        //  当前节点指向新的方向
        currentFlowNode.setOutgoingFlows(newSequenceFlowList);

        //  完成当前任务
        taskService.complete(task.getId());
    }

    /**
     * 获取当前任务列表
     * @param processInstanceId
     * @return
     */
    private List<Task> getCurrentTaskList(String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        return tasks;
    }
}



