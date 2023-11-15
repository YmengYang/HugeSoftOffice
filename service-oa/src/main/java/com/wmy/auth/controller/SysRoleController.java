package com.wmy.auth.controller;

import com.wmy.model.system.SysRole;
import com.wmy.vo.system.AssginRoleVo;
import com.wmy.vo.system.SysRoleQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wmy.auth.service.SysRoleService;
import com.wmy.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

//http:localhost:8800/admin/system/sysRole/findAll

@Api(tags = "角色管理")
@RestController
@RequestMapping("admin/system/sysRole")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    /**
     * 查询所有角色
     * @return
     */
    @PreAuthorize("hasAuthority('bnt.sysRole.list')")
    @ApiOperation(value = "获取所有角色列表")
    @GetMapping("/findAll")
    public Result findAll() { //统一返回数据结果
        List<SysRole> list = sysRoleService.list();
        return Result.ok(list);
    }

    /**
     * 条件分页查询
     */
    @PreAuthorize("hasAuthority('bnt.sysRole.list')")
    @ApiOperation(value = "条件分页查询")
    @GetMapping("{page}/{limit}")
    public Result pageQueryRole(@PathVariable Long page,
                                @PathVariable Long limit,
                                SysRoleQueryVo sysRoleQueryVo) {
        //1.创建page对象，传递分页相关参数
        Page<SysRole> pageParam = new Page<>(page, limit);

        //2.封装条件，判断条件是否为空，不为空进行封装
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        String roleName = sysRoleQueryVo.getRoleName();
        if (!StringUtils.isEmpty(roleName)) {
            //封装
            wrapper.like(SysRole::getRoleName,roleName);
        }

        //3.调用方法实现
        Page<SysRole> rolePage = sysRoleService.page(pageParam, wrapper);
        return Result.ok(rolePage);
    }

    /**
     * 添加角色
     */
    @PreAuthorize("hasAuthority('bnt.sysRole.add')")
    @ApiOperation("添加角色")
    @PostMapping("/save")
    public Result save(@RequestBody SysRole role) { //用json格式提交数据
        boolean is_success = sysRoleService.save(role);
        if (is_success) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    /**
     * 修改角色- 1.根据id查询
     */
    @PreAuthorize("hasAuthority('bnt.sysRole.list')")
    @ApiOperation("根据id查询")
    @GetMapping("/get/{id}")
    public Result getById(@PathVariable Long id) {
        SysRole sysRole = sysRoleService.getById(id);
        return Result.ok(sysRole);
    }

    /**
     * 修改角色- 2.最终修改
     */
    @PreAuthorize("hasAuthority('bnt.sysRole.update')")
    @ApiOperation("修改角色")
    @PostMapping("/update")
    public Result updateById(@RequestBody SysRole role) { //用json格式提交数据
        boolean is_success = sysRoleService.updateById(role);
        if (is_success) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    /**
     * 根据id删除
     */
    @PreAuthorize("hasAuthority('bnt.sysRole.remove')")
    @ApiOperation("根据id删除")
    @DeleteMapping("/remove/{id}")
    public Result removeById(@PathVariable Long id) {
        boolean is_success = sysRoleService.removeById(id);
        if (is_success) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    /**
     * 根据id批量删除
     */
    @PreAuthorize("hasAuthority('bnt.sysRole.remove')")
    @ApiOperation(value = "根据id列表删除")
    @DeleteMapping("/batchRemove")
    public Result batchRemove(@RequestBody List<Long> idList) { //java中的list集合接收json数组格式,将id以数组格式在前端传递
        boolean is_success = sysRoleService.removeByIds(idList);
        if (is_success) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    //下面两个接口用于给用户分配职位角色

    /**
     * 首先获取用户的所有角色
     */
    @ApiOperation("根据用户获取角色")
    @GetMapping("/toAssign/{userId}")
    public Result toAssign(@PathVariable Long userId) {
        Map<String, Object> roleMap = sysRoleService.findRoleByUserId(userId);
        return Result.ok(roleMap);
    }

    /**
     * 为用户分配角色
     */
    @ApiOperation(value = "根据用户分配角色")
    @PostMapping("/doAssign")
    public Result doAssign(@RequestBody AssginRoleVo assginRoleVo) {
        sysRoleService.doAssign(assginRoleVo);
        return Result.ok();
    }
}
