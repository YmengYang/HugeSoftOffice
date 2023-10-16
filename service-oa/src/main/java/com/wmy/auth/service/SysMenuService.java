package com.wmy.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wmy.model.system.SysMenu;
import com.wmy.vo.system.AssginMenuVo;
import com.wmy.vo.system.RouterVo;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {
    List<SysMenu> findNodes();

    void removeMenuById(Long id);

    List<SysMenu> finMenuByRoleId(Long roleId);

    void doAssign(AssginMenuVo assginMenuVo);

    List<RouterVo> findUserMenuListByUserId(Long userId);

    List<String> findUserPermByUserId(Long userId);
}
