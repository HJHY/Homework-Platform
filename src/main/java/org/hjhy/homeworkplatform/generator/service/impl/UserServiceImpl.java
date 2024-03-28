package org.hjhy.homeworkplatform.generator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.dto.PasswordDto;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.domain.User;
import org.hjhy.homeworkplatform.generator.mapper.UserMapper;
import org.hjhy.homeworkplatform.generator.service.UserService;
import org.hjhy.homeworkplatform.vo.SimpleUserVo;
import org.hjhy.homeworkplatform.vo.UserVo;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;

/**
 * @author 13746
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2024-02-04 13:44:16
 */
@Setter
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public List<SimpleUserVo> queryUsers(List<Integer> userIdList) {
        return this.listByIds(userIdList).stream().map(user -> {
            var simpleUserVo = new SimpleUserVo();
            BeanUtils.copyProperties(user, simpleUserVo);
            return simpleUserVo;
        }).toList();
    }

    @Override
    public UserVo queryUserInfo(Integer userId) {
        var user = this.getById(userId);
        var userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        return userVo;
    }

    @Override
    public void updateUserInfo(User user) {
        this.updateById(user);
    }

    @Override
    public void updatePassword(Integer userId, PasswordDto passwordDto) {
        //获取数据库中的密码
        var user = this.getById(userId);
        //检查密码
        if (ObjectUtils.isEmpty(user.getPassword())) {
            log.info("用户id为{{}}的用户未设置密码", userId);
            throw new BaseException(StatusCode.USER_PASSWORD_NOT_SEATED);
        }
        if (!BCrypt.checkpw(passwordDto.getOldPassword(), user.getPassword())) {
            throw new BaseException(StatusCode.MISMATCH_PASSWORD);
        }
        //设置新密码
        var newPassword = BCrypt.hashpw(passwordDto.getNewPassword(), BCrypt.gensalt());
        user.setPassword(newPassword);
        user.setLastUpdateTime(null);
        this.updateById(user);
    }
}




