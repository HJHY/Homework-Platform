package org.hjhy.homeworkplatform.generator.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.hjhy.homeworkplatform.dto.PasswordDto;
import org.hjhy.homeworkplatform.generator.domain.User;
import org.hjhy.homeworkplatform.vo.SimpleUserVo;
import org.hjhy.homeworkplatform.vo.UserVo;

import java.util.List;

/**
 * @author 13746
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2024-02-04 13:44:16
 */
public interface UserService extends IService<User> {
    List<SimpleUserVo> queryUsers(List<Integer> userIdList);

    UserVo queryUserInfo(Integer userId);

    void updateUserInfo(User user);


    void updatePassword(Integer userId, PasswordDto passwordDto);

}
