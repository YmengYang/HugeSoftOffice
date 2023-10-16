package com.wmy.auth.utils;

import com.wmy.model.system.SysMenu;

import java.util.ArrayList;
import java.util.List;

public class MenuHelper {

    public static List<SysMenu> buildTree(List<SysMenu> sysMenuList) {
        List<SysMenu> trees = new ArrayList<>();
        //把所有菜单数据进行遍历
        for (SysMenu sysMenu : sysMenuList) {
            //递归入口进入
            //菜单表字段parentId=0是入口
            if (sysMenu.getParentId().longValue() == 0) {
                trees.add(getChildren(sysMenu, sysMenuList));
            }
        }
        return trees;
    }

    /**
     * 用于一层一层向下找数据
     */
    public static SysMenu getChildren(SysMenu sysMenu, List<SysMenu> sysMenuList) {
        sysMenu.setChildren(new ArrayList<SysMenu>());
        //遍历所有菜单数据，判断id和parentId对应关系
        for (SysMenu menu : sysMenuList) {
            if (sysMenu.getId().longValue() == menu.getParentId().longValue()) {
                if (sysMenu.getChildren() == null) {
                    sysMenu.setChildren(new ArrayList<>());
                }
                sysMenu.getChildren().add(getChildren(menu, sysMenuList));
            }
        }
        return sysMenu;
    }
}
