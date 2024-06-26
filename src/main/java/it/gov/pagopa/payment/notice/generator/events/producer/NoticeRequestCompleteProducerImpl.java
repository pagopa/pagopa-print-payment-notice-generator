package it.gov.pagopa.payment.notice.generator.events.producer;

import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Service
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
        return streamBridge.send("noticeComplete-out-0",
                buildMessage(paymentNoticeGenerationRequest));
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
