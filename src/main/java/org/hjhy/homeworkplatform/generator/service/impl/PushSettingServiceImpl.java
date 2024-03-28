package org.hjhy.homeworkplatform.generator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.hjhy.homeworkplatform.generator.domain.PushSetting;
import org.hjhy.homeworkplatform.generator.mapper.PushSettingMapper;
import org.hjhy.homeworkplatform.generator.service.PushSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author 13746
 * @description 针对表【push_setting(推送设置表)】的数据库操作Service实现
 * @createDate 2024-02-08 16:50:07
 */
@Service
public class PushSettingServiceImpl extends ServiceImpl<PushSettingMapper, PushSetting> implements PushSettingService {

    @Override
    @Transactional
    public void setDefaultDdls(Integer userId) {
        //默认提前7天、3天和1天提醒
        var pushSettings = Stream.of(7, 3, 1).map(ddl -> PushSetting.builder().userId(userId).advanceTime(ddl * 24).build()).toList();
        this.saveBatch(pushSettings);
    }

    @Transactional
    @Override
    public void setDdls(Integer userId, List<Integer> ddlSettingList) {
        //删除所有的设置然后再重新设置
        this.remove(new LambdaQueryWrapper<PushSetting>().eq(PushSetting::getUserId, userId));
        var pushSettings = ddlSettingList.stream().map(ddl -> PushSetting.builder().userId(userId).advanceTime(ddl).build()).toList();
        this.saveBatch(pushSettings);
    }

    @Override
    public List<Integer> listDdls(Integer userId) {
        var pushSettingList = this.list(new LambdaQueryWrapper<PushSetting>().eq(PushSetting::getUserId, userId));
        return pushSettingList.stream().map(PushSetting::getAdvanceTime).toList();
    }
}




