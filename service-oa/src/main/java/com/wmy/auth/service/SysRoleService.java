package com.wmy.auth.service;

import com.wmy.model.system.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wmy.vo.system.AssginRoleVo;

import java.util.Map;

public interface SysRoleService extends IService<SysRole> {

    /**
     * 首先获取用户的所有角色
     */
    Map<String, Object> findRoleByUserId(Long userId);

    /**
     * 为用户分配角色
     */
    void doAssign(AssginRoleVo assginRoleVo);
}
