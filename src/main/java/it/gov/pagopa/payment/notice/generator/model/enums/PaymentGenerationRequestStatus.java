package it.gov.pagopa.payment.notice.generator.model.enums;

/**
 * Enum containing generation request status
 */
public enum PaymentGenerationRequestStatus {

    INSERTED,
    PROCESSING,
    COMPLETING,
    FAILED,
    PROCESSED,
    PROCESSED_WITH_FAILURES

}
