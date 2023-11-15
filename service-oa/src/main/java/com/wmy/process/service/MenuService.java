package com.wmy.process.service;

import com.wmy.model.wechat.Menu;
import com.wmy.vo.wechat.MenuVo;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface MenuService extends IService<Menu> {

    List<MenuVo> findMenuInfo();

    void syncMenu();

    void removeMenu();
}
