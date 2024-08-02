package it.gov.pagopa.payment.notice.generator.events.producer;

import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequestError;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Service
@Slf4j
public class NoticeRequestErrorProducerImpl implements NoticeRequestErrorProducer {

    private final StreamBridge streamBridge;

    public NoticeRequestErrorProducerImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public static Message<PaymentNoticeGenerationRequestError> buildMessage(
            PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError) {
        return MessageBuilder.withPayload(paymentNoticeGenerationRequestError).build();
    }

    @Override
    public boolean noticeError(PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError) {
        var res = streamBridge.send("noticeError-out-0",
                buildMessage(paymentNoticeGenerationRequestError));

        MDC.put("topic", "error");
        log.info("Error Message Sent");
        MDC.remove("topic");

        return res;
    }

    /**
     * Declared just to let know Spring to connect the producer at startup
     */
    @Slf4j
    @Configuration
    static class NoticeGenerationRequestErrorConfig {

        @Bean
        public Supplier<Flux<Message<PaymentNoticeGenerationRequestError>>> noticeError() {
            return Flux::empty;
        }

    }

}
