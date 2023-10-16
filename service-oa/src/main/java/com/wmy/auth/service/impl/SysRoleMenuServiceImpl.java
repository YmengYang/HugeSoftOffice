package com.wmy.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmy.auth.mapper.SysRoleMenuMapper;
import com.wmy.auth.service.SysRoleMenuService;
import com.wmy.model.system.SysRoleMenu;
import org.springframework.stereotype.Service;

@Service
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements SysRoleMenuService {
}
