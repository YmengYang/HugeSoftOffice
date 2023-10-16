package com.wmy.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wmy.auth.mapper.SysUserRoleMapper;
import com.wmy.model.system.SysRole;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmy.auth.mapper.SysRoleMapper;
import com.wmy.auth.service.SysRoleService;
import com.wmy.model.system.SysUserRole;
import com.wmy.vo.system.AssginRoleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    /**
     * 首先获取用户的所有角色
     */
    @Override
    public Map<String, Object> findRoleByUserId(Long userId) {
        //查询所有角色
        List<SysRole> allRolesList = this.list();

        //拥有的角色id
        List<SysUserRole> existUserRoleList = sysUserRoleMapper
                .selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
                        .select(SysUserRole::getRoleId));
        List<Long> existRoleIdList = existUserRoleList
                .stream().map(c->c.getRoleId()).collect(Collectors.toList());

        //对角色进行分配
        List<SysRole> assginRoleList = new ArrayList<>();
        for (SysRole role : allRolesList) {
            //已分配
            if (existRoleIdList.contains(role.getId())) {
                assginRoleList.add(role);
            }
        }
        HashMap<String, Object> roleMap = new HashMap<>();
        roleMap.put("assginRoleList", assginRoleList);
        roleMap.put("allRolesList", allRolesList);
        return roleMap;
    }

    /**
     * 为用户分配角色
     */
    @Transactional
    @Override
    public void doAssign(AssginRoleVo assginRoleVo) {
        //先把用户原有的职位删掉
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, assginRoleVo.getUserId()));

        for (Long roleId : assginRoleVo.getRoleIdList()) {
            if (StringUtils.isEmpty(roleId)) continue;
            SysUserRole sysUserRole = new SysUserRole();
            sysUserRole.setUserId(assginRoleVo.getUserId());
            sysUserRole.setRoleId(roleId);
            sysUserRoleMapper.insert(sysUserRole);
        }

    }

}
