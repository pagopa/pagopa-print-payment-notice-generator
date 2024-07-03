package it.gov.pagopa.payment.notice.generator.repository;

import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequestError;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentGenerationRequestErrorRepository
        extends MongoRepository<PaymentNoticeGenerationRequestError, String> {

    void deleteByErrorIdAndFolderId(String errorId, String folderId);

    Optional<PaymentNoticeGenerationRequestError> findByErrorIdAndFolderId(String errorId, String folderId);
}
