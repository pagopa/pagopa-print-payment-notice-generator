package it.gov.pagopa.payment.notice.generator.repository;

import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentGenerationRequestRepository extends MongoRepository<PaymentNoticeGenerationRequest, String> {

    @Query("{'id' : ?0}")
    @Update("{ '$set': { 'status' : 'PROCESSING' }, " +
            "'$inc' : { 'numberOfElementsProcessed' : 1 }, " +
            "'$addToSet' : { 'items' : '?1'} }")
    long findAndAddItemById(String folderId, String noticeId);

    @Update("{ '$inc' : { 'numberOfElementsFailed' : 1 } }")
    long findAndIncrementNumberOfElementsFailedById(String folderId);

}
