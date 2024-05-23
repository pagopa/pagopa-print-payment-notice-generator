package it.gov.pagopa.payment.notice.generator.events.producer;

import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequestError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoticeRequestErrorProducerImplTest {

    @Mock
    StreamBridge streamBridge;

    NoticeRequestErrorProducer noticeRequestErrorProducer;

    @BeforeEach
    public void init() {
        Mockito.reset(streamBridge);
        noticeRequestErrorProducer = new NoticeRequestErrorProducerImpl(streamBridge);
    }

    @Test
    void noticeError() {
        noticeRequestErrorProducer.noticeError(PaymentNoticeGenerationRequestError.builder().build());
        verify(streamBridge).send(any(), any());
    }

}
