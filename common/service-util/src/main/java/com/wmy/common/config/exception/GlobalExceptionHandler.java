package com.wmy.common.config.exception;

import com.wmy.common.result.Result;
import com.wmy.common.result.ResultCodeEnum;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 全局异常处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result error(Exception e){
        e.printStackTrace();
        return Result.fail();
    }

    /**
     * 特定异常处理
     */
    @ExceptionHandler(ArithmeticException.class)
    @ResponseBody
    public Result error(ArithmeticException e){
        e.printStackTrace();
        return Result.fail();
    }

    /**
     * 自定义异常处理
     */
    @ExceptionHandler(MyException.class)
    @ResponseBody
    public Result error(MyException e){
        e.printStackTrace();
        return Result.fail().message(e.getMessage()).code(e.getCode());
    }

    /**
     * spring security异常
     * @param e
     * @return
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public Result error(AccessDeniedException e) throws AccessDeniedException {
        return Result.fail().code(205).message("没有访问权限");
    }
}
