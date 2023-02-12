package com.nene.service.impl;

import com.nene.cache.RedisCache;
import com.nene.constants.RedisConstants;
import com.nene.domain.ResponseResult;
import com.nene.domain.dto.UserLoginDto;
import com.nene.domain.entity.User;
import com.nene.domain.vo.UserLoginVo;
import com.nene.enums.AppHttpCodeEnum;
import com.nene.exception.CustomServiceException;
import com.nene.service.BlogLoginService;
import com.nene.service.UserService;
import com.nene.utils.BeanCopyUtil;
import com.nene.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @ClassName BlogLoginServiceImpl
 * @Description 博客登录接口实现类
 * @Author Protip
 * @Date 2023/1/7 18:15
 * @Version 1.0
 */
@Service
@RequiredArgsConstructor
public class BlogLoginServiceImpl implements BlogLoginService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final RedisCache redisCache;

    @Override
    public ResponseResult login(UserLoginDto userLoginDto) {

        // 登录授权
        Authentication authentication = new UsernamePasswordAuthenticationToken(userLoginDto.getAccount(), userLoginDto.getPassword());
        Authentication authenticate = authenticationManager.authenticate(authentication);
        if (authenticate == null) {
            throw new CustomServiceException(AppHttpCodeEnum.LOGIN_ERROR);
        }

        // 查询用户信息
        User user = userService.lambdaQuery()
                .select(User::getId,
                        User::getUserName,
                        User::getNickName,
                        User::getPassword,
                        User::getType,
                        User::getStatus,
                        User::getEmail,
                        User::getPhoneNumber,
                        User::getSex,
                        User::getAvatar,
                        User::getCreateTime
                )
                .eq(User::getEmail, userLoginDto.getAccount())
                .or()
                .eq(User::getPhoneNumber, userLoginDto.getAccount())
                .or()
                .eq(User::getUserName, userLoginDto.getAccount())
                .one();

        // 在redis中缓存用户数据
        String key = RedisConstants.BLOG_LOGIN + user.getId();
        redisCache.setValue(key, user);
        long expiration = userLoginDto.isRemember() ? RedisConstants.TIMEOUT_REMEMBER : RedisConstants.TIMEOUT_DEFAULT;
        redisCache.expire(key, expiration, TimeUnit.HOURS);

        // 生成token返回
        String token = JwtUtil.getToken(user.getId());
        UserLoginVo.UserInfo userInfo = BeanCopyUtil.beanCopy(user, UserLoginVo.UserInfo.class);
        UserLoginVo userLoginVo = new UserLoginVo(token, userInfo);

        return ResponseResult.okResult(userLoginVo);
    }

    @Override
    public ResponseResult logout(User user) {
        String key = "BlogLogin_" + user.getId();
        redisCache.delValue(key);
        return ResponseResult.okResult("已成功注销！");
    }
}
