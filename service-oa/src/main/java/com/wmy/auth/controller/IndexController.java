package com.wmy.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wmy.auth.service.SysMenuService;
import com.wmy.auth.service.SysUserService;
import com.wmy.common.config.exception.MyException;
import com.wmy.common.jwt.JwtHelper;
import com.wmy.common.result.Result;
import com.wmy.common.utils.MD5;
import com.wmy.model.system.SysUser;
import com.wmy.vo.system.LoginVo;
import com.wmy.vo.system.RouterVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 后台登录登出
 * </p>
 */
@Api(tags = "后台登录管理")
@RestController
@RequestMapping("/admin/system/index")
public class IndexController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysMenuService sysMenuService;

    /**
     * 登录
     *
     * @return
     */
    @ApiOperation(value = "登录")
    @PostMapping("/login")
    public Result login(@RequestBody LoginVo loginVo) {
       /* Map<String, Object> map = new HashMap<>();
        map.put("token", "admin");
        return Result.ok(map);*/
        //获取用户名和密码，根据用户名查询
        String username = loginVo.getUsername();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser sysUser = sysUserService.getOne(wrapper);

        if (sysUser == null) {
            throw new MyException(201, "用户不存在");
        }

        //判断密码是否正确
        // 数据库密码（加密的）
        String password_db  = sysUser.getPassword();
        // 获取用户输入的密码
        String password_input = MD5.encrypt(loginVo.getPassword());
        if (!password_db.equals(password_input)) {
            throw new MyException(201, "密码错误");
        }

        if (sysUser.getStatus().intValue() == 0) {
            throw new MyException(201, "用户被禁用");
        }

        //使用Jwt根据用户id和用户名生成token字符串
        String token = JwtHelper.createToken(sysUser.getId(), sysUser.getUsername());

        HashMap<String, Object> map = new HashMap<>();
        map.put("token", token);
        return Result.ok(map);
    }

    /**
     * 获取用户信息
     *
     * @return
     */
    @GetMapping("/info")
    public Result info(HttpServletRequest request) {
        /*点击登录，执行controller里对应的login接口，在这个接口里会生成token字符串，
        前端把token放到cookie中，每次发送请求，token就在请求头里*/

        //1 从请求头获取用户信息（获取请求头Token字符串）
        String token = request.getHeader("token");

        //2 从token字符串获取用户id或者用户名称
        Long userId = JwtHelper.getUserId(token);

        //3 根据用户id查询数据库，把用户信息获取出来
        SysUser sysUser = sysUserService.getById(userId);

        //4 根据用户id获取用户可以操作的菜单列表
        //  查询数据库动态构建路由结构，进行显示
        List<RouterVo> routerList = sysMenuService.findUserMenuListByUserId(userId);

        //5 根据用户id获取用户可以操作的按钮列表
        List<String> permList = sysMenuService.findUserPermByUserId(userId);

        Map<String, Object> map = new HashMap<>();
        map.put("roles", "[admin]");
        map.put("name", sysUser.getName());
        map.put("avatar", "https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
        //返回用户可以操作的菜单
        map.put("routers", routerList);
        //返回用户可以操作按钮
        map.put("buttons", permList);
        return Result.ok(map);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    public Result logout() {
        return Result.ok();
    }

}
