package com.simplyviral.queue.configuration;

import com.simplyviral.orchestration.repository.JobRepository;
import com.simplyviral.queue.repository.StepQueueRepository;
import com.simplyviral.queue.service.DbQueueProvider;
import com.simplyviral.queue.service.QueueProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class QueueConfig {

    @Bean
    @ConditionalOnMissingBean(QueueProvider.class)
    @ConditionalOnProperty(name = "simplyviral.queue.mode", havingValue = "DB", matchIfMissing = true)
    public QueueProvider dbQueueProvider(StepQueueRepository queueRepository, JobRepository jobRepository) {
        log.info("Initializing QueueProvider with DB mode.");
        return new DbQueueProvider(queueRepository, jobRepository);
    }
}
