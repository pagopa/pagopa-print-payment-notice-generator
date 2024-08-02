package it.gov.pagopa.payment.notice.generator.events.producer;

import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequest;
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
public class NoticeRequestCompleteProducerImpl implements NoticeRequestCompleteProducer {

    private final StreamBridge streamBridge;

    public NoticeRequestCompleteProducerImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public static Message<PaymentNoticeGenerationRequest> buildMessage(
            PaymentNoticeGenerationRequest paymentNoticeGenerationRequest) {
        return MessageBuilder.withPayload(paymentNoticeGenerationRequest).build();
    }

    @Override
    public boolean noticeComplete(PaymentNoticeGenerationRequest paymentNoticeGenerationRequest) {
        var res = streamBridge.send("noticeComplete-out-0",
                buildMessage(paymentNoticeGenerationRequest));

        MDC.put("topic", "complete");
        MDC.put("action", "sent");
        log.info("Complete Message Sent");
        MDC.remove("topic");
        MDC.remove("action");

        return res;
    }

    /**
     * Declared just to let know Spring to connect the producer at startup
     */
    @Slf4j
    @Configuration
    static class NoticeGenerationRequestProducerConfig {

        @Bean
        public Supplier<Flux<Message<PaymentNoticeGenerationRequest>>> noticeComplete() {
            return Flux::empty;
        }

    }

}
