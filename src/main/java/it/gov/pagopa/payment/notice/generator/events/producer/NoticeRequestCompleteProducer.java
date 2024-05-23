package it.gov.pagopa.payment.notice.generator.events.producer;

import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequest;

/**
 * Interface to use when required to execute sending of a notice generation request through
 * the eventhub channel
 */
public interface NoticeRequestCompleteProducer {

    /**
     * Send notige generation request through EH
     *
     * @param paymentNoticeGenerationRequest data to send
     * @return boolean referring if the insertion on the sending channel was successfully
     */
    boolean noticeComplete(PaymentNoticeGenerationRequest paymentNoticeGenerationRequest);

}
