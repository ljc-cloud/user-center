package com.example.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.usercenter.common.BaseResponse;
import com.example.usercenter.common.ResultCode;
import com.example.usercenter.common.ResultUtils;
import com.example.usercenter.constant.UserConstant;
import com.example.usercenter.exception.BusinessException;
import com.example.usercenter.model.entity.User;
import com.example.usercenter.model.request.UserLoginRequest;
import com.example.usercenter.model.request.UserRegisterRequest;
import com.example.usercenter.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户接口
 * @author _LJC
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUsers(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.LOGIN_USER_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN, "无法获取当前用户");
        }
        // 返回脱敏对象
        User user = userService.getById(currentUser.getId());
        return ResultUtils.success(userService.getSafetyUser(user));
    }

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "请求参数为空");
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount,userPassword,checkPassword,planetCode)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "请求参数为空");
        }
        long res = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(res);
    }

    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "请求参数为空");
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount,userPassword)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "请求参数为空");
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        return ResultUtils.success(userService.userLogout(request));
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ResultCode.NO_AUTH, "该用户不是管理员");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> userCollect = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(userCollect);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ResultCode.NO_AUTH, "该用户不是管理员");
        }
        boolean res = userService.removeById(id);
        return ResultUtils.success(Boolean.TRUE.equals(res));
    }

    /**
     * 是否为管理员
     * @param request
     * @return
     */
    private boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.LOGIN_USER_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }
}
