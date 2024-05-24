package it.gov.pagopa.payment.notice.generator.events.producer;

import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoticeRequestCompleteProducerImplTest {

    @Mock
    StreamBridge streamBridge;

    NoticeRequestCompleteProducer noticeRequestCompleteProducer;

    @BeforeEach
    public void init() {
        Mockito.reset(streamBridge);
        noticeRequestCompleteProducer = new NoticeRequestCompleteProducerImpl(streamBridge);
    }

    @Test
    void noticeGeneration() {
        noticeRequestCompleteProducer.noticeComplete(PaymentNoticeGenerationRequest.builder().build());
        verify(streamBridge).send(any(), any());
    }

}
