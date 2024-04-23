package it.gov.pagopa.payment.notice.generator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequestError;
import it.gov.pagopa.payment.notice.generator.exception.Aes256Exception;
import it.gov.pagopa.payment.notice.generator.exception.AppError;
import it.gov.pagopa.payment.notice.generator.exception.AppException;
import it.gov.pagopa.payment.notice.generator.model.NoticeGenerationRequestItem;
import it.gov.pagopa.payment.notice.generator.repository.PaymentGenerationRequestErrorRepository;
import it.gov.pagopa.payment.notice.generator.repository.PaymentGenerationRequestRepository;
import it.gov.pagopa.payment.notice.generator.storage.InstitutionsStorageClient;
import it.gov.pagopa.payment.notice.generator.storage.NoticeStorageClient;
import it.gov.pagopa.payment.notice.generator.storage.NoticeTemplateStorageClient;
import it.gov.pagopa.payment.notice.generator.util.Aes256Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
public class NoticeGenerationServiceImpl implements NoticeGenerationService {

    private final PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository;
    private final PaymentGenerationRequestRepository paymentGenerationRequestRepository;

    private final InstitutionsStorageClient institutionsStorageClient;
    private final NoticeStorageClient noticeStorageClient;
    private final NoticeTemplateStorageClient noticeTemplateStorageClient;

    private final Aes256Utils aes256Utils;

    private final ObjectMapper objectMapper;

    public NoticeGenerationServiceImpl(
            PaymentGenerationRequestRepository paymentGenerationRequestRepository,
            PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository,
            InstitutionsStorageClient institutionsStorageClient,
            NoticeStorageClient noticeStorageClient,
            NoticeTemplateStorageClient noticeTemplateStorageClient,
            Aes256Utils aes256Utils,
            ObjectMapper objectMapper) {
        this.paymentGenerationRequestRepository = paymentGenerationRequestRepository;
        this.paymentGenerationRequestErrorRepository = paymentGenerationRequestErrorRepository;
        this.institutionsStorageClient = institutionsStorageClient;
        this.noticeStorageClient = noticeStorageClient;
        this.noticeTemplateStorageClient = noticeTemplateStorageClient;
        this.aes256Utils = aes256Utils;
        this.objectMapper = objectMapper;
    }

    @Override
    public File generateNotice(NoticeGenerationRequestItem noticeGenerationRequestItem,
                               String folderId) {

        if (folderId != null) {
            Optional<PaymentNoticeGenerationRequest> paymentNoticeGenerationRequestOptional =
                    paymentGenerationRequestRepository.findById(folderId);
            if (paymentNoticeGenerationRequestOptional.isEmpty()) {
                throw new AppException(AppError.FOLDER_NOT_AVAILABLE);
            }
        }

        try {
            File templateFile = noticeTemplateStorageClient.getTemplate(
                    noticeGenerationRequestItem.getTemplateId());

            File noticeFile = null;
            return noticeFile;

        } catch (Exception e) {
            if (folderId != null) {
                saveErrorEvent(folderId, noticeGenerationRequestItem);
            }
            throw e;
        }

    }

    private void saveErrorEvent(String folderId, NoticeGenerationRequestItem noticeGenerationRequestItem) {
        try {
            paymentGenerationRequestErrorRepository.save(
                    PaymentNoticeGenerationRequestError.builder()
                            .errorDescription("Encountered error sending notice on EH")
                            .folderId(folderId)
                            .data(aes256Utils.encrypt(objectMapper
                                    .writeValueAsString(noticeGenerationRequestItem)))
                            .createdAt(Instant.now())
                            .numberOfAttempts(0)
                            .build()
            );
            paymentGenerationRequestRepository.findAndIncrementNumberOfElementsFailedById(folderId);
        } catch (JsonProcessingException | Aes256Exception e) {
            log.error(
                    "Unable to save notice data into error repository for notice with folder " + folderId +
                            " and noticeId " + noticeGenerationRequestItem.getData().getNotice().getCode()
            );
        }
    }

}
