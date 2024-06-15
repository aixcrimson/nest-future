package com.atguigu.lease.web.app.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.atguigu.lease.common.constant.RedisConstant;
import com.atguigu.lease.web.app.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void getCode(String phone) {
        String randomCode = RandomUtil.randomNumbers(6);
        log.info("验证码为：{}", randomCode);
        stringRedisTemplate.opsForValue()
                .set(
                        RedisConstant.APP_LOGIN_PREFIX + phone,
                        randomCode,
                        RedisConstant.APP_LOGIN_CODE_RESEND_TIME_SEC,
                        TimeUnit.SECONDS
                );
    }
}
