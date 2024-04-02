package org.hjhy.homeworkplatform.generator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.hjhy.homeworkplatform.dto.ClassDto;
import org.hjhy.homeworkplatform.dto.ClazzConditionDto;
import org.hjhy.homeworkplatform.generator.domain.Clazz;
import org.hjhy.homeworkplatform.generator.domain.UserClassRole;
import org.hjhy.homeworkplatform.vo.ClassInfoVo;
import org.hjhy.homeworkplatform.vo.PageResult;

import java.util.List;

/**
 * @author 13746
 * @description 针对表【clazz(班级表)】的数据库操作Service
 * @createDate 2024-02-08 17:08:47
 */
public interface ClazzService extends IService<Clazz> {

    Clazz createClass(Integer userId, ClassDto classDto);

    void updateClass(Integer classId, ClassDto classDto);

    void deleteClass(Integer classId);

    Clazz findClass(Integer classId);

    List<ClassInfoVo> simpleInfo(List<Integer> classIdList);

    ClassInfoVo detail(Integer classId);

    String shareClass(Integer userId, Integer classId);

    void joinClass(Integer userId, String shareCode);

    void exitClass(Integer userId, Integer classId);

    List<Integer> queryClassAdmin(Integer classId);

    void setClassAdmin(Integer classId, List<Integer> userIdList);

    PageResult<ClassInfoVo> getJoinedClasses(Integer userId, Page<UserClassRole> page);

    PageResult<ClassInfoVo> getCreatedClasses(Integer userId, Page<Clazz> page) throws InterruptedException;

    PageResult<Clazz> condition(Page<Clazz> page, ClazzConditionDto ClazzConditionDto);

    void setClazzCache(Clazz clazz);

    Clazz getClazzFormCache(Integer classId);

    void invalidateCache(Integer classId);

    Clazz getClazzFromDB(Integer classId);

    Clazz getCachableClazz(Integer classId);

    void deleteClazzSync(Integer classId);
}
