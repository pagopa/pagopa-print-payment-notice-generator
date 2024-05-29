package it.gov.pagopa.payment.notice.generator.repository;

import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentGenerationRequestRepository extends MongoRepository<PaymentNoticeGenerationRequest, String> {

    @Query("{'id' : ?0}")
    @Update("{ '$set': { 'status' : 'PROCESSING' }, " +
            "'$addToSet' : { 'items' : '?1'} }")
    long findAndAddItemById(String folderId, String noticeId);

    @Query("{'id' : ?0, 'status' : 'PROCESSING' }")
    @Update("{ '$set': { 'status' : 'COMPLETING' } }")
    long findAndSetToComplete(String folderId);

    @Update("{ '$inc' : { 'numberOfElementsFailed' : 1 } }")
    long findAndIncrementNumberOfElementsFailedById(String folderId);

    @Query("{'id' : ?0, 'numberOfElementsFailed': { '$gte': 1 } }")
    @Update("{ '$inc' : { 'numberOfElementsFailed' : -1 } }")
    void findAndDecrementNumberOfElementsFailedById(String folderId);

}
