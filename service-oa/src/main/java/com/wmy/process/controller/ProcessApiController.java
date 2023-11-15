package com.wmy.process.controller;

import ch.qos.logback.classic.spi.EventArgUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wmy.auth.service.SysUserService;
import com.wmy.common.result.Result;
import com.wmy.model.process.Process;
import com.wmy.model.process.ProcessTemplate;
import com.wmy.model.process.ProcessType;
import com.wmy.process.service.ProcessService;
import com.wmy.process.service.ProcessTemplateService;
import com.wmy.process.service.ProcessTypeService;
import com.wmy.vo.process.ApprovalVo;
import com.wmy.vo.process.ProcessFormVo;
import com.wmy.vo.process.ProcessVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "审批流程管理")
@RestController
@RequestMapping(value = "/admin/process")
@CrossOrigin //跨域
public class ProcessApiController {

    @Autowired
    private ProcessTypeService processTypeService;

    @Autowired
    private ProcessTemplateService processTemplateService;

    @Autowired
    private ProcessService processService;

    @Autowired
    private SysUserService sysUserService;

    @ApiOperation(value = "启动流程")
    @PostMapping("/startUp")
    public Result startUp(@RequestBody ProcessFormVo processFormVo) {
        processService.startUp(processFormVo);
        return Result.ok();
    }

    /**
     * 获取审批模板数据
     */
    @ApiOperation(value = "获取审批模板")
    @GetMapping("getProcessTemplate/{processTemplateId}")
    public Result get(@PathVariable Long processTemplateId) {
        ProcessTemplate processTemplate = processTemplateService.getById(processTemplateId);
        return Result.ok(processTemplate);
    }

    /**
     * 查询所有审批分类和每个分类所有的审批模板
     */
    @GetMapping("findProcessType")
    public Result findProcessType() {
        List<ProcessType> list = processTypeService.findProcessType();
        return Result.ok(list);
    }

    @ApiOperation(value = "待处理")
    @GetMapping("/findPending/{page}/{limit}")
    public Result findPending(
            @ApiParam(name = "page", value = "当前页码", required = true)
            @PathVariable Long page,
            @ApiParam(name = "limit", value = "每页记录数", required = true)
            @PathVariable Long limit) {
        Page<Process> pageParam = new Page<>(page, limit);
        IPage<ProcessVo> pageModel = processService.findPending(pageParam);
        return Result.ok(pageModel);
    }

    @ApiOperation(value = "获取审批详情")
    @GetMapping("show/{id}")
    public Result show(@PathVariable Long id) {
        Map<String, Object> map = processService.show(id);
        return Result.ok(map);
    }

    @ApiOperation(value = "审批")
    @PostMapping("approve")
    public Result approve(@RequestBody ApprovalVo approvalVo) {
        processService.approve(approvalVo);
        return Result.ok();
    }

    @ApiOperation(value = "已处理")
    @GetMapping("/findProcessed/{page}/{limit}")
    public Result findProcessed(
            @ApiParam(name = "page", value = "当前页码", required = true)
            @PathVariable Long page,
            @ApiParam(name = "limit", value = "每页记录数", required = true)
            @PathVariable Long limit) {
        Page<Process> pageParam = new Page<>(page, limit);
        return Result.ok(processService.findProcessed(pageParam));
    }

    @ApiOperation(value = "已发起")
    @GetMapping("/findStarted/{page}/{limit}")
    public Result findStarted(
            @ApiParam(name = "page", value = "当前页码", required = true)
            @PathVariable Long page,
            @ApiParam(name = "limit", value = "每页记录数", required = true)
            @PathVariable Long limit) {
        Page<ProcessVo> pageParam = new Page<>(page, limit);
        IPage<ProcessVo> pageModel = processService.findStarted(pageParam);
        return Result.ok(pageModel);
    }

    @ApiOperation(value = "获取当前用户基本信息")
    @GetMapping("getCurrentUser")
    public Result getCurrentUser() {
        Map<String, Object> map = sysUserService.getCurrentUser();
        return Result.ok(map);
    }
}
