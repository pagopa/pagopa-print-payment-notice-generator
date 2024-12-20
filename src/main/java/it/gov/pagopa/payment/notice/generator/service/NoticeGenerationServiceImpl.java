package it.gov.pagopa.payment.notice.generator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;
import it.gov.pagopa.payment.notice.generator.client.PdfEngineClient;
import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequestError;
import it.gov.pagopa.payment.notice.generator.events.producer.NoticeRequestCompleteProducer;
import it.gov.pagopa.payment.notice.generator.events.producer.NoticeRequestErrorProducer;
import it.gov.pagopa.payment.notice.generator.exception.AppError;
import it.gov.pagopa.payment.notice.generator.exception.AppException;
import it.gov.pagopa.payment.notice.generator.mapper.TemplateDataMapper;
import it.gov.pagopa.payment.notice.generator.model.NoticeGenerationRequestItem;
import it.gov.pagopa.payment.notice.generator.model.NoticeRequestEH;
import it.gov.pagopa.payment.notice.generator.model.TemplateResource;
import it.gov.pagopa.payment.notice.generator.model.enums.PaymentGenerationRequestStatus;
import it.gov.pagopa.payment.notice.generator.model.notice.CreditorInstitution;
import it.gov.pagopa.payment.notice.generator.model.notice.Notice;
import it.gov.pagopa.payment.notice.generator.model.pdf.PdfEngineRequest;
import it.gov.pagopa.payment.notice.generator.model.pdf.PdfEngineResponse;
import it.gov.pagopa.payment.notice.generator.repository.PaymentGenerationRequestErrorRepository;
import it.gov.pagopa.payment.notice.generator.repository.PaymentGenerationRequestRepository;
import it.gov.pagopa.payment.notice.generator.storage.InstitutionsStorageClient;
import it.gov.pagopa.payment.notice.generator.storage.NoticeStorageClient;
import it.gov.pagopa.payment.notice.generator.storage.NoticeTemplateStorageClient;
import it.gov.pagopa.payment.notice.generator.util.Aes256Utils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static it.gov.pagopa.payment.notice.generator.util.CommonUtility.getItemId;
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

    private final NoticeRequestErrorProducer noticeRequestErrorProducer;

    public NoticeGenerationServiceImpl(
            PaymentGenerationRequestRepository paymentGenerationRequestRepository,
            PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository,
            InstitutionsStorageClient institutionsStorageClient,
            NoticeStorageClient noticeStorageClient,
            NoticeTemplateStorageClient noticeTemplateStorageClient,
            PdfEngineClient pdfEngineClient,
            Aes256Utils aes256Utils,
            ObjectMapper objectMapper,
            Validator validator, NoticeRequestCompleteProducer noticeRequestCompleteProducer, NoticeRequestErrorProducer noticeRequestErrorProducer) {
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
        this.noticeRequestErrorProducer = noticeRequestErrorProducer;
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
                               String folderId,
                               String errorId) {


        if(folderId != null) {
            findFolderIfExists(folderId);
        }

        String itemId = String.format("%s-%s-%s-%s", "pagopa-avviso",
                noticeGenerationRequestItem.getData().getCreditorInstitution().getTaxCode(),
                getNoticeCode(noticeGenerationRequestItem),
                noticeGenerationRequestItem.getTemplateId());
        MDC.put("itemStatus", "PROCESSING");
        log.info("Process a new Generation Event: {}", noticeGenerationRequestItem);
        MDC.remove("itemStatus");


        try {

            File workingDirectory = createWorkingDirectory();
            Path tempDirectory = Files.createTempDirectory(workingDirectory.toPath(), "notice-generator")
                    .normalize()
                    .toAbsolutePath();

            CreditorInstitution creditorInstitution = institutionsStorageClient.getInstitutionData(
                    noticeGenerationRequestItem.getData().getCreditorInstitution().getTaxCode());
            noticeGenerationRequestItem.getData().setCreditorInstitution(creditorInstitution);

            File templateFile = noticeTemplateStorageClient.getTemplate(
                    noticeGenerationRequestItem.getTemplateId());

            TemplateResource templateResource = noticeTemplateStorageClient.getTemplates().stream()
                    .filter(item -> item.getTemplateId().equals(noticeGenerationRequestItem.getTemplateId()))
                    .findFirst()
                    .orElse(null);

            validateTemplate(noticeGenerationRequestItem, templateResource);

            String templateData = objectMapper.writeValueAsString(
                    TemplateDataMapper.mapTemplate(noticeGenerationRequestItem.getData()));

            //Build the request
            PdfEngineRequest request = new PdfEngineRequest();
            request.setTemplate(templateFile.toURI().toURL());
            request.setData(templateData);
            request.setApplySignature(false);

            PdfEngineResponse pdfEngineResponse = callPdfEngine(request, tempDirectory);

            if(folderId != null) {
                addNoticeIntoFolder(itemId, folderId, pdfEngineResponse);
                if(errorId != null) {
                    paymentGenerationRequestErrorRepository.deleteByErrorIdAndFolderId(errorId, folderId);
                    paymentGenerationRequestRepository.findAndDecrementNumberOfElementsFailedById(folderId);
                    MDC.put("itemStatus", "RECOVERED");
                    log.info("Recovered Generation Event - errorId: {}", errorId);
                    MDC.remove("itemStatus");
                }
            }

            return new File(pdfEngineResponse.getTempPdfPath());

        } catch (Exception e) {
            if(folderId != null) {
                saveErrorEvent(errorId, itemId, folderId, noticeGenerationRequestItem, e.getMessage());
            }

            if(e instanceof AppException) {
                throw e;
            }

            throw new AppException(AppError.INTERNAL_SERVER_ERROR, e);
        }

    }


    /**
     * Retrieves the notice code, given a {@link NoticeGenerationRequestItem}.
     * If the notice has a code, it is returned. Otherwise, the code of the first
     * installment is returned. If no installments are present, a random UUID is generated.
     *
     * @param noticeGenerationRequestItem the {@link NoticeGenerationRequestItem}
     * @return the notice code
     */
    private static String getNoticeCode(NoticeGenerationRequestItem noticeGenerationRequestItem) {
        Notice notice = noticeGenerationRequestItem.getData().getNotice();
        String code = null;
        if(notice.getCode() != null) {
            code = notice.getCode();
        }

        if(notice.getReduced() != null) {
            code = notice.getReduced().getCode();
        }

        if(notice.getDiscounted() != null) {
            code = notice.getDiscounted().getCode();
        }

        if(notice.getInstallments() != null && !notice.getInstallments().isEmpty()) {
            code = notice.getInstallments().get(0).getCode();
        }

        return Optional.ofNullable(code)
                .orElse(UUID.randomUUID().toString());
    }


    private PdfEngineResponse callPdfEngine(PdfEngineRequest request, Path tempDirectory) {
        PdfEngineResponse pdfEngineResponse = pdfEngineClient.generatePDF(request, tempDirectory);

        if(pdfEngineResponse.getStatusCode() != HttpStatus.SC_OK) {
            String errMsg = String.format("PDF-Engine response KO (%s): %s", pdfEngineResponse.getStatusCode(), pdfEngineResponse.getErrorMessage());
            log.error(errMsg);
            throw new AppException(AppError.PDF_ENGINE_ERROR, errMsg);
        }
        return pdfEngineResponse;
    }

    /**
     * This method valid the request against the validation rules
     *
     * @param noticeGenerationRequestItem the request to validate
     * @param templateResource            the json schema with the validation rules
     * @throws JsonProcessingException if template is not readable as json
     */
    private void validateTemplate(NoticeGenerationRequestItem noticeGenerationRequestItem, TemplateResource templateResource) throws JsonProcessingException {
        if(templateResource != null && templateResource.getTemplateValidationRules() != null) {
            JsonSchema jsonSchema = JsonSchemaFactory
                    .getInstance(SpecVersion.VersionFlag.V7)
                    .getSchema(templateResource.getTemplateValidationRules());
            String jsonStringSchema = objectMapper.writeValueAsString(noticeGenerationRequestItem.getData());

            Set<ValidationMessage> validationMessageSet = jsonSchema.validate(jsonStringSchema, InputFormat.JSON);
            // check if there are validation messages
            if(!validationMessageSet.isEmpty()) {
                List<String> value = validationMessageSet.stream()
                        .map(ValidationMessage::getMessage)
                        .toList();
                throw new AppException(AppError.BAD_REQUEST, objectMapper.writeValueAsString(value));
            }
        }

    }

    private void addNoticeIntoFolder(String itemId, String folderId,
                                     PdfEngineResponse pdfEngineResponse) {
        try (BufferedInputStream pdfStream = new BufferedInputStream(
                new FileInputStream(pdfEngineResponse.getTempPdfPath()))) {

            if(!noticeStorageClient.savePdfToBlobStorage(pdfStream, folderId, itemId)) {
                throw new RuntimeException("Encountered error during blob saving");
            }

            paymentGenerationRequestRepository.findAndAddItemById(folderId, itemId);
            MDC.put("massiveStatus", "PROCESSING");
            log.info("Massive Request PROCESSING: {}", folderId);
            MDC.remove("massiveStatus");

            var paymentNoticeGenerationRequest = paymentGenerationRequestRepository.findById(folderId)
                    .orElseThrow();

            if(paymentNoticeGenerationRequest.getStatus().equals(PaymentGenerationRequestStatus.PROCESSING)
                    && paymentNoticeGenerationRequest.getNumberOfElementsTotal()
                    <= paymentNoticeGenerationRequest.getItems().size() + paymentNoticeGenerationRequest.getNumberOfElementsFailed()
                    && paymentGenerationRequestRepository.findAndSetToComplete(folderId) > 0) {
                paymentNoticeGenerationRequest.setStatus(PaymentGenerationRequestStatus.COMPLETING);
                noticeRequestCompleteProducer.noticeComplete(paymentNoticeGenerationRequest);
                MDC.put("massiveStatus", "COMPLETING");
                log.info("Massive Request COMPLETING: {}", folderId);
                MDC.remove("massiveStatus");
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
        MDC.clear();


        String folderId = null;
        NoticeGenerationRequestItem noticeGenerationRequestItem = null;
        String errorId = null;
        NoticeRequestEH noticeRequestEH = null;

        try {

            noticeRequestEH = objectMapper.readValue(message, NoticeRequestEH.class);
            MDC.put("folderId", noticeRequestEH.getFolderId());
            MDC.put("topic", "generation");
            MDC.put("action", "received");
            MDC.put("itemId", getItemId(noticeRequestEH));
            log.info("Received Generation Message: notice {}", getItemId(noticeRequestEH));
            MDC.remove("topic");
            MDC.remove("action");

            log.info("Pre-Process a new Generation Request Event: {}", noticeRequestEH);

            Set<ConstraintViolation<NoticeRequestEH>> constraintValidators = validator.validate(noticeRequestEH);
            if(!constraintValidators.isEmpty()) {
                MDC.put("itemStatus", "EXCEPTION");
                log.error("Exception Generation Event: {}", AppError.MESSAGE_VALIDATION_ERROR.getTitle());
                MDC.remove("itemStatus");
                throw new AppException(AppError.MESSAGE_VALIDATION_ERROR, objectMapper.writeValueAsString(
                        constraintValidators.stream().map(ConstraintViolation::getMessage).toList()));
            }

            folderId = noticeRequestEH.getFolderId();
            noticeGenerationRequestItem = noticeRequestEH.getNoticeData();
            errorId = noticeRequestEH.getErrorId();

        } catch (JsonProcessingException e) {
            try {
                paymentGenerationRequestErrorRepository.save(
                        PaymentNoticeGenerationRequestError.builder()
                                .errorDescription("Unable to read EH message content")
                                .folderId("UNKNOWN")
                                .data(message != null ? aes256Utils.encrypt(message) : "EMPTY")
                                .createdAt(Instant.now())
                                .numberOfAttempts(0)
                                .compressionError(false)
                                .build());
                MDC.put("itemStatus", "FAILED");
                log.info("Failed Generation Event: {}", e.getMessage(), e);
                MDC.remove("itemStatus");
            } catch (Exception cryptException) {
                MDC.put("itemStatus", "EXCEPTION");
                log.error("Exception Generation Event: Unable to save unparsable data to error", cryptException);
                MDC.remove("itemStatus");
            }
        }

        try {
            if(noticeGenerationRequestItem != null && folderId != null) {
                generateNotice(noticeGenerationRequestItem, folderId, errorId);
                MDC.put("itemStatus", "SUCCESS");
                log.info("Success Generation Event: {}", noticeRequestEH);
                MDC.remove("itemStatus");
            }
        } catch (Exception e) {
            MDC.put("itemStatus", "EXCEPTION");
            log.error("Exception Generation Event: {}", e.getMessage(), e);
            MDC.remove("itemStatus");
            throw e;
        }

    }

    private void saveErrorEvent(
            String errorId, String itemId, String folderId,
            NoticeGenerationRequestItem noticeGenerationRequestItem,
            String error) {

        try {

            PaymentNoticeGenerationRequestError toSave =
                    paymentGenerationRequestErrorRepository.findByErrorIdAndFolderId(errorId != null ?
                            errorId : itemId, folderId).orElse(null);

            if(toSave == null) {
                toSave = PaymentNoticeGenerationRequestError.builder()
                        .errorId(itemId)
                        .errorDescription(error)
                        .folderId(folderId)
                        .data(aes256Utils.encrypt(objectMapper
                                .writeValueAsString(noticeGenerationRequestItem)))
                        .createdAt(Instant.now())
                        .numberOfAttempts(0)
                        .compressionError(false)
                        .build();
                paymentGenerationRequestRepository.findAndIncrementNumberOfElementsFailedById(folderId);
            }
            toSave.setErrorDescription(error);
            PaymentNoticeGenerationRequestError paymentNoticeGenerationRequestError =
                    paymentGenerationRequestErrorRepository.save(toSave);
            noticeRequestErrorProducer.noticeError(paymentNoticeGenerationRequestError);
            MDC.put("itemStatus", "FAILED");
            log.info("Failed Generation Event: {}", toSave);
            MDC.remove("itemStatus");
        } catch (Exception e) {
            log.error("Unable to save notice data into error repository for notice with folder {} and noticeId {}",
                    folderId,
                    getNoticeCode(noticeGenerationRequestItem),
                    e
            );
        }
    }

    private void findFolderIfExists(String folderId) {
        PaymentNoticeGenerationRequest ignored =
                paymentGenerationRequestRepository.findById(folderId)
                        .orElseThrow(() -> new AppException(AppError.FOLDER_NOT_AVAILABLE));
    }

}
