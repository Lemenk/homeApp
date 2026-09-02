package com.familyhome.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.familyhome.common.BizException;
import com.familyhome.entity.User;
import com.familyhome.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 用户服务：用户查询与注册 */
@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 按 ID 查询用户 */
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return user;
    }

    /** 按手机号查询用户 */
    public User findByPhone(String phone) {
        return userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone));
    }

    /**
     * 手机号登录：已存在则返回，否则自动注册。
     */
    public User loginOrCreateByPhone(String phone) {
        User user = findByPhone(phone);
        if (user != null) {
            return user;
        }
        return createUser(phone);
    }

    /** 创建新用户 */
    public User createUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }
}
