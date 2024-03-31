package org.hjhy.homeworkplatform.generator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.hjhy.homeworkplatform.generator.domain.ConsumedMessage;
import org.hjhy.homeworkplatform.generator.mapper.ConsumedMessagesMapper;
import org.hjhy.homeworkplatform.generator.service.ConsumedMessagesService;
import org.springframework.stereotype.Service;

/**
 * @author 13746
 * @description 针对表【consumed_messages(消息防重表)】的数据库操作Service实现
 * @createDate 2024-03-31 21:24:24
 */
@Service
public class ConsumedMessagesServiceImpl extends ServiceImpl<ConsumedMessagesMapper, ConsumedMessage>
        implements ConsumedMessagesService {

}




