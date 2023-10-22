package com.wmy.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmy.model.process.ProcessTemplate;
import com.wmy.model.process.ProcessType;
import com.wmy.process.mapper.ProcessTemplateMapper;
import com.wmy.process.service.ProcessTemplateService;
import com.wmy.process.service.ProcessTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcessTemplateServiceImpl extends ServiceImpl<ProcessTemplateMapper, ProcessTemplate> implements ProcessTemplateService {

    @Autowired
    private ProcessTypeService processTypeService;

    @Override
    public IPage<ProcessTemplate> selectPageProcessTempate(Page<ProcessTemplate> pageParam) {
        //1.调用mapper的方法实现分页查询
        Page<ProcessTemplate> processTemplatePage = baseMapper.selectPage(pageParam, null);

        //2.第一步分页查询返回数据，从数据里获取列表list集合
        List<ProcessTemplate> processTemplateList = processTemplatePage.getRecords();

        //3.遍历list集合，得到每个对象的审批类型id
        for (ProcessTemplate processTemplate : processTemplateList) {
            //得到每个对象的审批类型id
            Long processTypeId = processTemplate.getProcessTypeId();
            //4 根据审批类型id，查询获取对应名称
            LambdaQueryWrapper<ProcessType> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProcessType::getId, processTypeId);
            ProcessType processType = processTypeService.getOne(wrapper);
            if(processType == null) {
                continue;
            }
            //5.完成最终封装processTypeName
            processTemplate.setProcessTypeName(processType.getName());
        }

        return processTemplatePage;
    }

    /**
     * 流程定义部署
     * @param id
     */
    @Override
    public void publish(Long id) {
        //修改模板发布状态 1 已发布
        ProcessTemplate processTemplate = baseMapper.selectById(id);
        processTemplate.setStatus(1);
        baseMapper.updateById(processTemplate);
    }
}
