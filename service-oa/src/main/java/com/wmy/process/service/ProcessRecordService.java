package com.wmy.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wmy.model.process.ProcessRecord;

public interface ProcessRecordService extends IService<ProcessRecord> {

    void record(Long processId, Integer status, String description);
}
