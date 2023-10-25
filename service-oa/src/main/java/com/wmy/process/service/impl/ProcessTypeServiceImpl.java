package com.wmy.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmy.model.process.ProcessTemplate;
import com.wmy.model.process.ProcessType;
import com.wmy.process.mapper.ProcessTypeMapper;
import com.wmy.process.service.ProcessTemplateService;
import com.wmy.process.service.ProcessTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcessTypeServiceImpl extends ServiceImpl<ProcessTypeMapper, ProcessType> implements ProcessTypeService {

    @Autowired
    private ProcessTemplateService processTemplateService;

    /**
     * 查询所有审批分类和每个分类所有的审批模板
     */
    @Override
    public List<ProcessType> findProcessType() {
        //1.查询所有审批分类，返回list集合
        List<ProcessType> processTypeList = baseMapper.selectList(null);

        //2.遍历返回所有审批分类list集合
        for (ProcessType processType : processTypeList) {
            //3.得到每个审批分类，根据审批分类id查询对应审批模板
            Long typeId = processType.getId();
            LambdaQueryWrapper<ProcessTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProcessTemplate::getProcessTypeId,typeId);
            List<ProcessTemplate> processTemplateList = processTemplateService.list(wrapper);

            //4 根据审批分类id查询对应审批模板数据（List）封装到每个审批分类对象里面
            processType.setProcessTemplateList(processTemplateList);
        }
        return processTypeList;
    }
}
