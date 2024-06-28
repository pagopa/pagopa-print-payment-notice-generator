package it.gov.pagopa.payment.notice.generator.repository;

import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequestError;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentGenerationRequestErrorRepository
        extends MongoRepository<PaymentNoticeGenerationRequestError, String> {

    void deleteByErrorId(String errorId);

    Optional<PaymentNoticeGenerationRequestError> findByErrorId(String errorId);
}
