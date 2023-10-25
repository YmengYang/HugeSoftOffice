package com.wmy.process.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmy.auth.service.SysUserService;
import com.wmy.model.process.ProcessRecord;
import com.wmy.model.system.SysUser;
import com.wmy.process.mapper.ProcessRecordMapper;
import com.wmy.process.service.ProcessRecordService;
import com.wmy.security.custom.LoginUserInfoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProcessRecordServiceImpl extends ServiceImpl<ProcessRecordMapper, ProcessRecord> implements ProcessRecordService {

    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private ProcessRecordMapper processRecordMapper;

    @Override
    public void record(Long processId, Integer status, String description) {
        Long userId = LoginUserInfoHelper.getUserId();
        SysUser sysUser = sysUserService.getById(userId);
        ProcessRecord processRecord = new ProcessRecord();
        processRecord.setProcessId(processId);
        processRecord.setStatus(status);
        processRecord.setDescription(description);

        processRecord.setOperateUser(sysUser.getName());
        processRecord.setOperateUserId(userId);
        processRecordMapper.insert(processRecord);
    }
}
