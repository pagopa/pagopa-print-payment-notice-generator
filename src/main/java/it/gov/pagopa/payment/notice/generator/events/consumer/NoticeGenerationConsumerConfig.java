package it.gov.pagopa.payment.notice.generator.events.consumer;

import it.gov.pagopa.payment.notice.generator.service.NoticeGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class NoticeGenerationConsumerConfig {
    @Bean
    public Consumer<String> noticeGeneration(NoticeGenerationService noticeGenerationService){
        return noticeGenerationService::processNoticeGenerationEH;
    }
}
