package com.wmy.process.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmy.model.process.ProcessType;
import com.wmy.process.mapper.ProcessTypeMapper;
import com.wmy.process.service.ProcessTypeService;
import org.springframework.stereotype.Service;

@Service
public class ProcessTypeServiceImpl extends ServiceImpl<ProcessTypeMapper, ProcessType> implements ProcessTypeService {

}
