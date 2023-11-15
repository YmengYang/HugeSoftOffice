package com.wmy.process.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wmy.common.result.Result;
import com.wmy.model.process.Process;
import com.wmy.process.service.ProcessService;
import com.wmy.vo.process.ProcessQueryVo;
import com.wmy.vo.process.ProcessVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "审批流管理")
@RestController
@RequestMapping(value = "/admin/process")
public class ProcessController {

    @Autowired
    private ProcessService processService;

    /**
     * 分页查询审批管理列表
     * @param page
     * @param limit
     * @param processQueryVo
     * @return
     */
    @ApiOperation(value = "获取分页列表")
    @GetMapping("{page}/{limit}")
    public Result index(@PathVariable Long page, @PathVariable Long limit, ProcessQueryVo processQueryVo) {
        Page<ProcessVo> pageParam = new Page<>(page, limit);
        IPage<ProcessVo> pageModel = processService.selectPage(pageParam, processQueryVo);
        return Result.ok(pageModel);
    }
}
