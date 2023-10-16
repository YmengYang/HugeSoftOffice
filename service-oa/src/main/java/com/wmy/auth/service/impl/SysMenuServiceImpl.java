package com.wmy.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wmy.auth.mapper.SysMenuMapper;
import com.wmy.auth.service.SysMenuService;
import com.wmy.auth.service.SysRoleMenuService;
import com.wmy.auth.utils.MenuHelper;
import com.wmy.common.config.exception.MyException;
import com.wmy.model.system.SysMenu;
import com.wmy.model.system.SysRoleMenu;
import com.wmy.vo.system.AssginMenuVo;
import com.wmy.vo.system.MetaVo;
import com.wmy.vo.system.RouterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Autowired
    private SysRoleMenuService sysRoleMenuService;

    /**
     * 菜单列表接口
     *
     * @return
     */
    public List<SysMenu> findNodes() {
        //1查询所有菜单数据
        List<SysMenu> sysMenus = baseMapper.selectList(null);
        if (CollectionUtils.isEmpty(sysMenus)) return null;

        //2把数据构建成树形结构
        /*{
            第一层
                    children:[
                       {
                           第二层
                                   ...
                       }
                    ]
        }*/
        //构建树形数据
        List<SysMenu> result = MenuHelper.buildTree(sysMenus);
        return result;
    }

    /**
     * 删除菜单
     *
     * @param id
     */
    @Override
    public void removeMenuById(Long id) {
        //判断当前菜单是否有下一层菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, id);
        Integer count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new MyException(201, "菜单无法删除");
        }
        baseMapper.deleteById(id);
    }

    /**
     * 根据角色获取权限菜单
     *
     * @param roleId
     * @return
     */
    @Override
    public List<SysMenu> finMenuByRoleId(Long roleId) {
        //1 查询所有权限列表，有一个查询条件status=1表示可用
        LambdaQueryWrapper<SysMenu> wrapperSysMenu = new LambdaQueryWrapper<>();
        wrapperSysMenu.eq(SysMenu::getStatus, 1);
        List<SysMenu> allSysMenuList = baseMapper.selectList(wrapperSysMenu);

        //2 根据sys_role_menu表的role_id和menu_id查询一个职位对应的所有的菜单权限
        //  即根据role_id查询它所有的menu_id
        LambdaQueryWrapper<SysRoleMenu> wrapperSysRoleMenu = new LambdaQueryWrapper<>();
        wrapperSysRoleMenu.eq(SysRoleMenu::getRoleId, roleId);
        List<SysRoleMenu> sysRoleMenuList = sysRoleMenuService.list(wrapperSysRoleMenu);

        //3 转换给角色id与角色权限对应Map对象
        List<Long> menuIdList = sysRoleMenuList.stream().map(e -> e.getMenuId()).collect(Collectors.toList());

        //用所有的菜单id到菜单集合里比较id，相同的进行封装
        allSysMenuList.stream().forEach(item -> {
            if (menuIdList.contains(item.getId())) {
                item.setSelect(true);
            } else {
                item.setSelect(false);
            }
        });

        //4 返回规定树形显示格式菜单列表
        List<SysMenu> sysMenuList = MenuHelper.buildTree(allSysMenuList);
        return sysMenuList;
    }

    /**
     * 为角色分配权限菜单
     *
     * @param assginMenuVo
     */
    @Override
    public void doAssign(AssginMenuVo assginMenuVo) {
        //1.根据角色id删除sys_role_menu表里原来分配的数据
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, assginMenuVo.getRoleId());
        sysRoleMenuService.remove(wrapper);

        //2 从参数里面获取角色新分配的权限id列表，进行遍历，把每个id数据添加到菜单角色表
        for (Long menuId : assginMenuVo.getMenuIdList()) {
            if (StringUtils.isEmpty(menuId)) continue;
            SysRoleMenu sysRoleMenu = new SysRoleMenu();
            sysRoleMenu.setRoleId(assginMenuVo.getRoleId());
            sysRoleMenu.setMenuId(menuId);
            sysRoleMenuService.save(sysRoleMenu);
        }

    }

    /**
     * 根据用户id查询用户有的菜单
     *
     * @param userId
     * @return
     */
    @Override
    public List<RouterVo> findUserMenuListByUserId(Long userId) {
        List<SysMenu> sysMenuList = null;
        //1 判断当前用户是管理员还是其它用户 userId=1是管理员
        //1.1 如果是管理员，查询所有菜单列表
        if (userId.longValue() == 1) {
            //查询所有菜单列表
            LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysMenu::getStatus, 1);
            sysMenuList = baseMapper.selectList(wrapper);
        } else {
            //1.2 普通用户根据用户id查询可以操作的菜单列表，多表查询用SQL
            sysMenuList = baseMapper.findMenuListByUserId(userId);
        }

        //2 把查询的菜单列表构建成框架要求的路由数据结构
        List<SysMenu> sysMenuTreeList = MenuHelper.buildTree(sysMenuList);
        List<RouterVo> routerVoList = this.buildRouter(sysMenuTreeList);
        return routerVoList;
    }

    /**
     * 根据菜单构建路由
     * @param menus
     * @return
     */
    private List<RouterVo> buildRouter(List<SysMenu> menus) {
        List<RouterVo> routers = new LinkedList<RouterVo>();
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            router.setHidden(false);
            router.setAlwaysShow(false);
            router.setPath(getRouterPath(menu));
            router.setComponent(menu.getComponent());
            router.setMeta(new MetaVo(menu.getName(), menu.getIcon()));
            List<SysMenu> children = menu.getChildren();
            //如果当前是菜单，需将按钮对应的路由加载出来，如：“角色授权”按钮对应的路由在“系统管理”下面
            if(menu.getType().intValue() == 1) {
                List<SysMenu> hiddenMenuList = children.stream().filter(item -> !StringUtils.isEmpty(item.getComponent())).collect(Collectors.toList());
                for (SysMenu hiddenMenu : hiddenMenuList) {
                    RouterVo hiddenRouter = new RouterVo();
                    hiddenRouter.setHidden(true);
                    hiddenRouter.setAlwaysShow(false);
                    hiddenRouter.setPath(getRouterPath(hiddenMenu));
                    hiddenRouter.setComponent(hiddenMenu.getComponent());
                    hiddenRouter.setMeta(new MetaVo(hiddenMenu.getName(), hiddenMenu.getIcon()));
                    routers.add(hiddenRouter);
                }
            } else {
                if (!CollectionUtils.isEmpty(children)) {
                    if(children.size() > 0) {
                        router.setAlwaysShow(true);
                    }
                    router.setChildren(buildRouter(children));
                }
            }
            routers.add(router);
        }
        return routers;
    }

    /**
     * 获取路由地址
     *
     * @param menu 菜单信息
     * @return 路由地址
     */
    public String getRouterPath(SysMenu menu) {
        String routerPath = "/" + menu.getPath();
        if(menu.getParentId().intValue() != 0) {
            routerPath = menu.getPath();
        }
        return routerPath;
    }

        /**
         * 根据用户id获取用户可以操作的按钮列表
         * @param userId
         * @return
         */
        @Override
        public List<String> findUserPermByUserId (Long userId) {
            //1 判断当前用户是管理员还是其它用户 userId=1是管理员
            List<SysMenu> sysMenuList = null;
            if (userId.longValue() == 1) {
                //查询所有菜单列表
                LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SysMenu::getStatus, 1);
                sysMenuList = baseMapper.selectList(wrapper);
            } else {
                //2 普通用户根据用户id查询可以操作的按钮列表，多表查询用SQL
                sysMenuList = baseMapper.findMenuListByUserId(userId);
            }

            //3 从查询出来的数据里面，获取可以操作按钮的list，返回
            List<String> permsList = sysMenuList.stream()
                    .filter(item -> item.getType() == 2) // type=2的才是操作按钮
                    .map(item -> item.getPerms())
                    .collect(Collectors.toList());
            return permsList;
        }
    }


