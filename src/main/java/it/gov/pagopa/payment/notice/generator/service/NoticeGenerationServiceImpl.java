package it.gov.pagopa.payment.notice.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.notice.generator.client.PdfEngineClient;
import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequestError;
import it.gov.pagopa.payment.notice.generator.events.producer.NoticeRequestCompleteProducer;
import it.gov.pagopa.payment.notice.generator.exception.AppError;
import it.gov.pagopa.payment.notice.generator.exception.AppException;
import it.gov.pagopa.payment.notice.generator.mapper.TemplateDataMapper;
import it.gov.pagopa.payment.notice.generator.model.NoticeGenerationRequestItem;
import it.gov.pagopa.payment.notice.generator.model.NoticeRequestEH;
import it.gov.pagopa.payment.notice.generator.model.notice.CreditorInstitution;
import it.gov.pagopa.payment.notice.generator.model.pdf.PdfEngineRequest;
import it.gov.pagopa.payment.notice.generator.model.pdf.PdfEngineResponse;
import it.gov.pagopa.payment.notice.generator.repository.PaymentGenerationRequestErrorRepository;
import it.gov.pagopa.payment.notice.generator.repository.PaymentGenerationRequestRepository;
import it.gov.pagopa.payment.notice.generator.storage.InstitutionsStorageClient;
import it.gov.pagopa.payment.notice.generator.storage.NoticeStorageClient;
import it.gov.pagopa.payment.notice.generator.storage.NoticeTemplateStorageClient;
import it.gov.pagopa.payment.notice.generator.util.Aes256Utils;
import jakarta.validation.Validator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static it.gov.pagopa.payment.notice.generator.util.CommonUtility.sanitizeLogParam;
import static it.gov.pagopa.payment.notice.generator.util.WorkingDirectoryUtils.createWorkingDirectory;

/**
 * Services regarding the notice generation flow
 */
@Service
@Slf4j
public class NoticeGenerationServiceImpl implements NoticeGenerationService {

    private final PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository;
    private final PaymentGenerationRequestRepository paymentGenerationRequestRepository;

    private final PdfEngineClient pdfEngineClient;

    private final InstitutionsStorageClient institutionsStorageClient;
    private final NoticeStorageClient noticeStorageClient;
    private final NoticeTemplateStorageClient noticeTemplateStorageClient;

    private final Aes256Utils aes256Utils;

    private final ObjectMapper objectMapper;

    private final Validator validator;

    private final NoticeRequestCompleteProducer noticeRequestCompleteProducer;

    public NoticeGenerationServiceImpl(
            PaymentGenerationRequestRepository paymentGenerationRequestRepository,
            PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository,
            InstitutionsStorageClient institutionsStorageClient,
            NoticeStorageClient noticeStorageClient,
            NoticeTemplateStorageClient noticeTemplateStorageClient,
            PdfEngineClient pdfEngineClient,
            Aes256Utils aes256Utils,
            ObjectMapper objectMapper,
            Validator validator, NoticeRequestCompleteProducer noticeRequestCompleteProducer) {
        this.paymentGenerationRequestRepository = paymentGenerationRequestRepository;
        this.paymentGenerationRequestErrorRepository = paymentGenerationRequestErrorRepository;
        this.institutionsStorageClient = institutionsStorageClient;
        this.noticeStorageClient = noticeStorageClient;
        this.noticeTemplateStorageClient = noticeTemplateStorageClient;
        this.pdfEngineClient = pdfEngineClient;
        this.aes256Utils = aes256Utils;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.noticeRequestCompleteProducer = noticeRequestCompleteProducer;
    }

    /**
     * Generate a notice, if required content is provided and valid, and saves the content to a folderId if provided
     * and valid
     *
     * @param noticeGenerationRequestItem request data to use for the notice generation
     * @param folderId                    optional parameter to generate folderId
     * @return generated notice
     */
    @SneakyThrows
    @Override
    public File generateNotice(NoticeGenerationRequestItem noticeGenerationRequestItem,
                               String folderId) {

        if(folderId != null) {
            findFolderIfExists(folderId);
        }

        Path tempDirectory;

        try {

            File workingDirectory = createWorkingDirectory();
            tempDirectory = Files.createTempDirectory(workingDirectory.toPath(), "notice-generator")
                    .normalize().toAbsolutePath();

            File templateFile = noticeTemplateStorageClient.getTemplate(
                    noticeGenerationRequestItem.getTemplateId());

            CreditorInstitution creditorInstitution = institutionsStorageClient.getInstitutionData(
                    noticeGenerationRequestItem.getData().getCreditorInstitution().getTaxCode());
            noticeGenerationRequestItem.getData().setCreditorInstitution(creditorInstitution);

            PdfEngineRequest request = new PdfEngineRequest();

            //Build the request
            request.setTemplate(templateFile.toURI().toURL());
            request.setData(objectMapper.writeValueAsString(
                    TemplateDataMapper.mapTemplate(noticeGenerationRequestItem.getData())));
            request.setApplySignature(false);

            PdfEngineResponse pdfEngineResponse = pdfEngineClient.generatePDF(request, tempDirectory);

            if(pdfEngineResponse.getStatusCode() != HttpStatus.SC_OK) {
                String errMsg = String.format("PDF-Engine response KO (%s): %s",
                        pdfEngineResponse.getStatusCode(), pdfEngineResponse.getErrorMessage());
                log.error(errMsg);
                throw new AppException(AppError.PDF_ENGINE_ERROR, errMsg);
            }

            if(folderId != null) {
                addNoticeIntoFolder(noticeGenerationRequestItem, folderId, pdfEngineResponse);
            }

            return new File(pdfEngineResponse.getTempPdfPath());

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if(folderId != null) {
                saveErrorEvent(folderId, noticeGenerationRequestItem, e.getMessage());
            }
            throw e;
        }

    }

    private void addNoticeIntoFolder(NoticeGenerationRequestItem noticeGenerationRequestItem, String folderId, PdfEngineResponse pdfEngineResponse) {
        try (BufferedInputStream pdfStream = new BufferedInputStream(
                new FileInputStream(pdfEngineResponse.getTempPdfPath()))) {

            String dateFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
            String blobName = String.format("%s-%s-%s", "pagopa-avviso", dateFormatted,
                    noticeGenerationRequestItem.getData().getNotice().getCode());
            if(!noticeStorageClient.savePdfToBlobStorage(pdfStream, folderId, blobName)) {
                throw new RuntimeException("Encountered error during blob saving");
            }

            paymentGenerationRequestRepository.findAndAddItemById(folderId, blobName);
            PaymentNoticeGenerationRequest paymentNoticeGenerationRequest =
                    paymentGenerationRequestRepository.findById(folderId).orElseThrow();
            if (Objects.equals(paymentNoticeGenerationRequest.getNumberOfElementsTotal(),
                    paymentNoticeGenerationRequest.getNumberOfElementsProcessed() +
                            paymentNoticeGenerationRequest.getNumberOfElementsFailed()) &&
                paymentGenerationRequestRepository.findAndSetToComplete(folderId) > 0) {
                    noticeRequestCompleteProducer.noticeGeneration(paymentNoticeGenerationRequest);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AppException(AppError.NOTICE_SAVE_ERROR, e);
        }
    }

    /**
     * Generate a notice provided as a EH message
     *
     * @param message content to use for generation process
     */
    @Override
    public void processNoticeGenerationEH(String message) {

        String folderId = null;
        NoticeGenerationRequestItem noticeGenerationRequestItem = null;

        try {

            NoticeRequestEH noticeRequestEH = objectMapper.readValue(message, NoticeRequestEH.class);

            if(!validator.validate(noticeRequestEH).isEmpty()) {
                throw new AppException(AppError.MESSAGE_VALIDATION_ERROR);
            }

            folderId = noticeRequestEH.getFolderId();
            noticeGenerationRequestItem = noticeRequestEH.getNoticeData();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            try {
                paymentGenerationRequestErrorRepository.save(
                        PaymentNoticeGenerationRequestError.builder()
                                .errorDescription("Unable to read EH message content")
                                .folderId("UNKNOWN")
                                .data(message != null ? aes256Utils.encrypt(message) : "EMPTY")
                                .createdAt(Instant.now())
                                .numberOfAttempts(0)
                                .build());
            } catch (Exception cryptException) {
                log.error(
                        "Unable to save unparsable data to error"
                );
            }
        }

        if(noticeGenerationRequestItem != null && folderId != null) {
            generateNotice(noticeGenerationRequestItem, folderId);
        }

    }

    private void saveErrorEvent(
            String folderId, NoticeGenerationRequestItem noticeGenerationRequestItem, String error) {
        try {
            paymentGenerationRequestErrorRepository.save(
                    PaymentNoticeGenerationRequestError.builder()
                            .errorDescription(error)
                            .folderId(folderId)
                            .data(aes256Utils.encrypt(objectMapper
                                    .writeValueAsString(noticeGenerationRequestItem)))
                            .createdAt(Instant.now())
                            .numberOfAttempts(0)
                            .build()
            );
            paymentGenerationRequestRepository.findAndIncrementNumberOfElementsFailedById(folderId);
        } catch (Exception e) {
            log.error("Unable to save notice data into error repository for notice with folder {} and noticeId {}",
                    folderId,
                    sanitizeLogParam(noticeGenerationRequestItem.getData().getNotice().getCode())

            );
        }
    }

    private void findFolderIfExists(String folderId) {
        PaymentNoticeGenerationRequest ignored =
                paymentGenerationRequestRepository.findById(folderId)
                .orElseThrow(() -> new AppException(AppError.FOLDER_NOT_AVAILABLE));
    }

}
