package com.wmy.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wmy.model.process.ProcessType;

import java.util.List;

public interface ProcessTypeService extends IService<ProcessType> {
    List<ProcessType> findProcessType();
}
