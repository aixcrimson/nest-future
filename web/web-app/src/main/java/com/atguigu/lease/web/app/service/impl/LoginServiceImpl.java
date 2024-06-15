package com.atguigu.lease.web.app.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.atguigu.lease.common.constant.RedisConstant;
import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.common.utils.JwtUtil;
import com.atguigu.lease.model.entity.UserInfo;
import com.atguigu.lease.model.enums.BaseStatus;
import com.atguigu.lease.web.app.mapper.UserInfoMapper;
import com.atguigu.lease.web.app.service.LoginService;
import com.atguigu.lease.web.app.vo.user.LoginVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
    @Autowired
    private UserInfoMapper userInfoMapper;

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

    @Override
    public String login(LoginVo loginVo) {
        // 1.从Redis中获取验证码，若为空，返回错误
        String redisCode = stringRedisTemplate.opsForValue().get(RedisConstant.APP_LOGIN_PREFIX + loginVo.getPhone());
        if(StrUtil.isBlank(redisCode)){
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_CODE_EXPIRED);
        }
        // 2.判断验证码是否一致
        if(!redisCode.equals(loginVo.getCode())){
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_CODE_ERROR);
        }
        // 3.根据手机号查询数据库，如果未查到，新建用户
        UserInfo user = userInfoMapper.selectOne(new QueryWrapper<UserInfo>().eq("phone", loginVo.getPhone()));
        if(user == null){
            user = new UserInfo();
            user.setPhone(loginVo.getPhone());
            user.setStatus(BaseStatus.ENABLE);
            user.setNickname(RandomUtil.randomString("用户", 6));
            userInfoMapper.insert(user);
        }
        // 4.判断用户是否已禁用
        if(user.getStatus() == BaseStatus.DISABLE){
            throw new LeaseException(ResultCodeEnum.APP_ACCOUNT_DISABLED_ERROR);
        }
        // 5.生成token
        String token = JwtUtil.createToken(user.getId(), user.getPhone());
        // 6.返回
        return token;
    }
}
