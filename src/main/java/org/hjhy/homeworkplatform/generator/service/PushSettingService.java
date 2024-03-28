package org.hjhy.homeworkplatform.generator.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.hjhy.homeworkplatform.generator.domain.PushSetting;

import java.util.List;

/**
 * @author 13746
 * @description 针对表【push_setting(推送设置表)】的数据库操作Service
 * @createDate 2024-02-08 16:50:07
 */
public interface PushSettingService extends IService<PushSetting> {

    void setDefaultDdls(Integer userId);

    void setDdls(Integer userId, List<Integer> ddlSettingList);

    List<Integer> listDdls(Integer userId);
}
