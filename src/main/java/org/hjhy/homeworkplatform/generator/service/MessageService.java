package org.hjhy.homeworkplatform.generator.service;

import org.hjhy.homeworkplatform.dto.EmailDto;
import org.hjhy.homeworkplatform.dto.HomeworkPackagedFileCleanDto;
import org.hjhy.homeworkplatform.dto.HomeworkReminderDto;
import org.hjhy.homeworkplatform.dto.HomeworkStatusDto;

/**
 * @author HJHY
 * @date 2024/2/4 14:47
 */
public interface MessageService {
    void sendEmail(EmailDto emailDto);

    void checkHomeworkStatus(HomeworkStatusDto homeworkStatusDto);

    void cleanPackagedFile(HomeworkPackagedFileCleanDto homeworkPackagedFileCleanDto);

    void sendHomeworkDelayReminderEmail(HomeworkReminderDto homeworkReminderDto, long delay);
}
