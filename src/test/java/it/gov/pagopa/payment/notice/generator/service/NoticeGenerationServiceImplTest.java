package it.gov.pagopa.payment.notice.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.notice.generator.client.PdfEngineClient;
import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.payment.notice.generator.events.producer.NoticeRequestCompleteProducer;
import it.gov.pagopa.payment.notice.generator.events.producer.NoticeRequestErrorProducer;
import it.gov.pagopa.payment.notice.generator.exception.AppException;
import it.gov.pagopa.payment.notice.generator.exception.CompletionEventPublicationException;
import it.gov.pagopa.payment.notice.generator.model.NoticeGenerationRequestItem;
import it.gov.pagopa.payment.notice.generator.model.NoticeRequestEH;
import it.gov.pagopa.payment.notice.generator.model.TemplateResource;
import it.gov.pagopa.payment.notice.generator.model.enums.PaymentGenerationRequestStatus;
import it.gov.pagopa.payment.notice.generator.model.notice.*;
import it.gov.pagopa.payment.notice.generator.model.pdf.PdfEngineResponse;
import it.gov.pagopa.payment.notice.generator.repository.PaymentGenerationRequestErrorRepository;
import it.gov.pagopa.payment.notice.generator.repository.PaymentGenerationRequestRepository;
import it.gov.pagopa.payment.notice.generator.storage.InstitutionsStorageClient;
import it.gov.pagopa.payment.notice.generator.storage.NoticeStorageClient;
import it.gov.pagopa.payment.notice.generator.storage.NoticeTemplateStorageClient;
import it.gov.pagopa.payment.notice.generator.util.Aes256Utils;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeGenerationServiceImplTest {

    @Mock
    PaymentGenerationRequestRepository paymentGenerationRequestRepository;

    @Mock
    PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository;

    @Mock
    InstitutionsStorageClient institutionsStorageClient;

    @Mock
    NoticeStorageClient noticeStorageClient;

    @Mock
    NoticeTemplateStorageClient noticeTemplateStorageClient;

    @Mock
    PdfEngineClient pdfEngineClient;

    @Mock
    NoticeRequestCompleteProducer noticeRequestCompleteProducer;

    @Mock
    NoticeRequestErrorProducer noticeRequestErrorProducer;

    ObjectMapper objectMapper = new ObjectMapper();

    NoticeGenerationServiceImpl noticeGenerationService;

    File templateFile;

    File noticeFile;

    Validator validator;

    @SneakyThrows
    NoticeGenerationServiceImplTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        File tempDirectory = new File("temp");
        if (!tempDirectory.exists()) {
            Files.createDirectory(tempDirectory.toPath());
        }

        templateFile = File.createTempFile("tempFile", ".txt", tempDirectory);
        noticeFile = File.createTempFile("notice", ".tmp", tempDirectory);

    }

    @BeforeEach
    void init() {
        Mockito.reset(paymentGenerationRequestErrorRepository, paymentGenerationRequestRepository,
                institutionsStorageClient, noticeStorageClient, noticeTemplateStorageClient, pdfEngineClient);
        lenient().when(noticeTemplateStorageClient.getTemplates()).thenReturn(Collections.emptyList());
        noticeGenerationService = new NoticeGenerationServiceImpl(paymentGenerationRequestRepository,
                paymentGenerationRequestErrorRepository, institutionsStorageClient, noticeStorageClient,
                noticeTemplateStorageClient, pdfEngineClient, new Aes256Utils("test", "test"), objectMapper, validator,
                noticeRequestCompleteProducer, noticeRequestErrorProducer);
    }

    @SneakyThrows
    @Test
    void processNoticeGenerationShouldReturnOkOnValidData() {

        doReturn(templateFile).when(noticeTemplateStorageClient).getTemplate(any());
        doReturn(CreditorInstitution.builder().webChannel(true).physicalChannel("Test").fullName("Test").logo("logo")
                .cbill("Cbill").organization("ORG").build()).when(institutionsStorageClient).getInstitutionData(any());
        doReturn(getPdfEngineResponse(HttpStatus.SC_OK, noticeFile.getPath())).when(pdfEngineClient).generatePDF(any(),
                any());
        doReturn(true).when(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());
        doReturn(1L).when(paymentGenerationRequestRepository).findAndAddItemById(any(), any());
        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().status(PaymentGenerationRequestStatus.PROCESSING)
                .numberOfElementsTotal(1).numberOfElementsFailed(0).items(Collections.singletonList("test")).build()))
                .when(paymentGenerationRequestRepository).findById(any());
        doReturn(1L).when(paymentGenerationRequestRepository).findAndSetToComplete(any());
        doReturn(true).when(noticeRequestCompleteProducer).noticeComplete(any());

        NoticeRequestEH noticeRequestEH = NoticeRequestEH.builder().folderId("test")
                .noticeData(
                        NoticeGenerationRequestItem.builder().templateId("template")
                                .data(NoticeRequestData.builder().notice(Notice.builder().code("code")
                                        .dueDate("24/10/2024").subject("subject").paymentAmount(100L)
                                        .reduced(InstallmentData.builder().amount(100L).code("codeRate")
                                                .dueDate("24/10/2024").build())
                                        .discounted(InstallmentData.builder().amount(100L).code("codeRate")
                                                .dueDate("24/10/2024").build())
                                        .installments(Collections.singletonList(InstallmentData.builder().amount(100L)
                                                .code("codeRate").dueDate("24/10/2024").build()))
                                        .build())
                                        .creditorInstitution(CreditorInstitution.builder().taxCode("taxCode").build())
                                        .debtor(Debtor.builder().taxCode("taxCode").address("address").city("city")
                                                .buildingNumber("101").postalCode("00135").province("RM")
                                                .fullName("Test Name").build())
                                        .build())
                                .build())
                .build();
        noticeGenerationService.processNoticeGenerationEH(objectMapper.writeValueAsString(noticeRequestEH));
        /*
         * The Kafka consumer thread may be reused after processing the message. Folder
         * and item context must therefore not leak outside the consumer boundary.
         */
        assertNull(MDC.get("folderId"));
        assertNull(MDC.get("itemId"));
        verify(paymentGenerationRequestRepository).findAndAddItemById(any(), any());
        verify(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());
        verify(institutionsStorageClient).getInstitutionData(any());
        verify(noticeTemplateStorageClient).getTemplate(any());
        verify(pdfEngineClient).generatePDF(any(), any());
        verifyNoInteractions(paymentGenerationRequestErrorRepository);
    }

    @SneakyThrows
    @Test
    void processNoticeGenerationShouldReturnKOOnPDfEngineBadRequest() {

        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().build())).when(paymentGenerationRequestRepository)
                .findById(any());
        doReturn(templateFile).when(noticeTemplateStorageClient).getTemplate(any());
        doReturn(CreditorInstitution.builder().webChannel(true).physicalChannel("Test").fullName("Test").logo("logo")
                .cbill("Cbill").organization("ORG").posteAccountNumber("131213").posteAuth("322323").build())
                .when(institutionsStorageClient).getInstitutionData(any());
        doReturn(getPdfEngineResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, noticeFile.getPath())).when(pdfEngineClient)
                .generatePDF(any(), any());

        NoticeRequestEH noticeRequestEH = NoticeRequestEH.builder().folderId("test")
                .noticeData(NoticeGenerationRequestItem.builder().templateId("template")
                        .data(NoticeRequestData.builder()
                                .notice(Notice.builder().code("code").dueDate("24/10/2024").subject("subject")
                                        .paymentAmount(100L)
                                        .installments(Collections.singletonList(InstallmentData.builder().amount(100L)
                                                .code("codeRate").dueDate("24/10/2024").build()))
                                        .build())
                                .creditorInstitution(CreditorInstitution.builder().taxCode("taxCode").build())
                                .debtor(Debtor.builder().taxCode("taxCode").address("address").city("city")
                                        .buildingNumber("101").postalCode("00135").province("RM").fullName("Test Name")
                                        .build())
                                .build())
                        .build())
                .build();
        String message = objectMapper.writeValueAsString(noticeRequestEH);
        assertThrows(AppException.class, () -> noticeGenerationService.processNoticeGenerationEH(message));
        verify(paymentGenerationRequestRepository).findById(any());
        verify(institutionsStorageClient).getInstitutionData(any());
        verify(noticeTemplateStorageClient).getTemplate(any());
        verify(pdfEngineClient).generatePDF(any(), any());
        verify(paymentGenerationRequestErrorRepository).save(any());
        verify(paymentGenerationRequestRepository).findAndIncrementNumberOfElementsFailedById(any());
        verifyNoInteractions(noticeStorageClient);
    }

    @SneakyThrows
    @Test
    void processNoticeGenerationShouldReturnKoOnInvalidData() {

        NoticeRequestEH noticeRequestEH = NoticeRequestEH.builder()
                .noticeData(NoticeGenerationRequestItem.builder().templateId("template")
                        .data(NoticeRequestData.builder()
                                .notice(Notice.builder().code("code").dueDate("24/10/2024").subject("subject")
                                        .paymentAmount(100L)
                                        .installments(Collections.singletonList(InstallmentData.builder().amount(100L)
                                                .code("codeRate").dueDate("24/10/2024").build()))
                                        .build())
                                .creditorInstitution(CreditorInstitution.builder().taxCode("taxCode").build())
                                .debtor(Debtor.builder().taxCode("taxCode").address("address").city("city")
                                        .buildingNumber("101").postalCode("00135").province("RM").fullName("Test Name")
                                        .build())
                                .build())
                        .build())
                .build();
        String message = objectMapper.writeValueAsString(noticeRequestEH);
        assertThrows(AppException.class, () -> noticeGenerationService.processNoticeGenerationEH(message));
    }

    @SneakyThrows
    @Test
    void processNoticeGenerationShouldReturnKOOnPDfEngineBadRequestWithRepoException() {

        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().build())).when(paymentGenerationRequestRepository)
                .findById(any());
        doAnswer(item -> {
            throw new Exception("Could not increment data");
        }).when(paymentGenerationRequestRepository).findAndIncrementNumberOfElementsFailedById(any());
        doReturn(templateFile).when(noticeTemplateStorageClient).getTemplate(any());
        doReturn(Optional.empty()).when(paymentGenerationRequestErrorRepository).findByErrorIdAndFolderId(any(), any());
        doReturn(CreditorInstitution.builder().webChannel(true).physicalChannel("Test").fullName("Test").logo("logo")
                .cbill("Cbill").organization("ORG").posteAccountNumber("131213").posteAuth("322323").build())
                .when(institutionsStorageClient).getInstitutionData(any());
        doReturn(getPdfEngineResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, noticeFile.getPath())).when(pdfEngineClient)
                .generatePDF(any(), any());

        NoticeRequestEH noticeRequestEH = NoticeRequestEH.builder().folderId("test")
                .noticeData(NoticeGenerationRequestItem.builder().templateId("template")
                        .data(NoticeRequestData.builder()
                                .notice(Notice.builder().code("code").dueDate("24/10/2024").subject("subject")
                                        .paymentAmount(100L)
                                        .installments(Collections.singletonList(InstallmentData.builder().amount(100L)
                                                .code("codeRate").dueDate("24/10/2024").build()))
                                        .build())
                                .creditorInstitution(CreditorInstitution.builder().taxCode("taxCode").build())
                                .debtor(Debtor.builder().taxCode("taxCode").address("address").city("city")
                                        .buildingNumber("101").postalCode("00135").province("RM").fullName("Test Name")
                                        .build())
                                .build())
                        .build())
                .build();

        String message = objectMapper.writeValueAsString(noticeRequestEH);
        assertThrows(AppException.class, () -> noticeGenerationService.processNoticeGenerationEH(message));

        verify(paymentGenerationRequestRepository).findById(any());
        verify(institutionsStorageClient).getInstitutionData(any());
        verify(noticeTemplateStorageClient).getTemplate(any());
        verify(pdfEngineClient).generatePDF(any(), any());
        verify(paymentGenerationRequestRepository).findAndIncrementNumberOfElementsFailedById(any());
        verify(paymentGenerationRequestErrorRepository).findByErrorIdAndFolderId(any(), any());
        verifyNoInteractions(noticeStorageClient);
    }

    @SneakyThrows
    @Test
    void processNoticeGenerationShouldReturnKOOnExtraValidation() {

        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().build())).when(paymentGenerationRequestRepository)
                .findById(any());
        doReturn(CreditorInstitution.builder().webChannel(true).physicalChannel("Test").fullName("Test").logo("logo")
                .cbill("Cbill").organization("ORG").posteAccountNumber("131213").posteAuth("322323").build())
                .when(institutionsStorageClient).getInstitutionData(any());
        when(noticeTemplateStorageClient.getTemplates()).thenReturn(Collections.singletonList(TemplateResource.builder()
                .templateId("template")
                .templateValidationRules(
                        "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"title\":\"Default notice validation schema\","
                                + "\"description\":\"Default validation schema\",\"required\":[\"extra\",\"debtor\",\"payee\",\"notice\"],"
                                + "\"properties\":{\"debtor\":{\"type\":\"object\"},\"payee\":{\"type\":\"object\"},"
                                + "\"notice\":{\"type\":\"object\",\"required\":[\"qrCode\"],"
                                + "\"properties\":{\"qrCode\":{\"type\":\"string\"}}}}}")
                .build()));

        NoticeRequestEH noticeRequestEH = NoticeRequestEH.builder().folderId("test")
                .noticeData(NoticeGenerationRequestItem.builder().templateId("template")
                        .data(NoticeRequestData.builder()
                                .notice(Notice.builder().code("code").dueDate("24/10/2024").subject("subject")
                                        .paymentAmount(100L)
                                        .installments(Collections.singletonList(InstallmentData.builder().amount(100L)
                                                .code("codeRate").dueDate("24/10/2024").build()))
                                        .build())
                                .creditorInstitution(CreditorInstitution.builder().taxCode("taxCode").build())
                                .debtor(Debtor.builder().taxCode("taxCode").address("address").city("city")
                                        .buildingNumber("101").postalCode("00135").province("RM").fullName("Test Name")
                                        .build())
                                .build())
                        .build())
                .build();

        String message = objectMapper.writeValueAsString(noticeRequestEH);
        assertThrows(AppException.class, () -> noticeGenerationService.processNoticeGenerationEH(message));

        verify(paymentGenerationRequestRepository).findById(any());
        verify(institutionsStorageClient).getInstitutionData(any());
        verify(paymentGenerationRequestErrorRepository).save(any());
        verify(paymentGenerationRequestRepository).findAndIncrementNumberOfElementsFailedById(any());
        verifyNoInteractions(noticeStorageClient);
        verifyNoInteractions(pdfEngineClient);
    }

    @SneakyThrows
    @Test
    void processNoticeGenerationShouldReturnOkOnValidDataWithExtraValidation() {

        doReturn(templateFile).when(noticeTemplateStorageClient).getTemplate(any());
        when(noticeTemplateStorageClient.getTemplates()).thenReturn(Collections.singletonList(TemplateResource.builder()
                .templateId("template")
                .templateValidationRules(
                        "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"title\":\"Default notice validation schema\","
                                + "\"description\":\"Default validation schema\",\"required\":[\"debtor\",\"creditorInstitution\",\"notice\"],"
                                + "\"properties\":{\"debtor\":{\"type\":\"object\"},\"creditorInstitution\":{\"type\":\"object\"},"
                                + "\"notice\":{\"type\":\"object\"" + "}}}}}")
                .build()));
        doReturn(CreditorInstitution.builder().webChannel(true).physicalChannel("Test").fullName("Test").logo("logo")
                .cbill("Cbill").organization("ORG").build()).when(institutionsStorageClient).getInstitutionData(any());
        doReturn(getPdfEngineResponse(HttpStatus.SC_OK, noticeFile.getPath())).when(pdfEngineClient).generatePDF(any(),
                any());
        doReturn(true).when(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());
        doReturn(1L).when(paymentGenerationRequestRepository).findAndAddItemById(any(), any());
        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().status(PaymentGenerationRequestStatus.PROCESSING)
                .numberOfElementsTotal(1).numberOfElementsFailed(0).items(Collections.singletonList("test")).build()))
                .when(paymentGenerationRequestRepository).findById(any());
        doReturn(1L).when(paymentGenerationRequestRepository).findAndSetToComplete(any());
        doReturn(true).when(noticeRequestCompleteProducer).noticeComplete(any());

        NoticeRequestEH noticeRequestEH = NoticeRequestEH.builder().folderId("test")
                .noticeData(
                        NoticeGenerationRequestItem.builder().templateId("template")
                                .data(NoticeRequestData.builder().notice(Notice.builder().code("code")
                                        .dueDate("24/10/2024").subject("subject").paymentAmount(100L)
                                        .reduced(InstallmentData.builder().amount(100L).code("codeRate")
                                                .dueDate("24/10/2024").build())
                                        .discounted(InstallmentData.builder().amount(100L).code("codeRate")
                                                .dueDate("24/10/2024").build())
                                        .installments(Collections.singletonList(InstallmentData.builder().amount(100L)
                                                .code("codeRate").dueDate("24/10/2024").build()))
                                        .build())
                                        .creditorInstitution(CreditorInstitution.builder().taxCode("taxCode").build())
                                        .debtor(Debtor.builder().taxCode("taxCode").address("address").city("city")
                                                .buildingNumber("101").postalCode("00135").province("RM")
                                                .fullName("Test Name").build())
                                        .build())
                                .build())
                .build();
        noticeGenerationService.processNoticeGenerationEH(objectMapper.writeValueAsString(noticeRequestEH));
        verify(paymentGenerationRequestRepository).findAndAddItemById(any(), any());
        verify(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());
        verify(institutionsStorageClient).getInstitutionData(any());
        verify(noticeTemplateStorageClient).getTemplate(any());
        verify(pdfEngineClient).generatePDF(any(), any());
        verifyNoInteractions(paymentGenerationRequestErrorRepository);
    }

    @SneakyThrows
    @Test
    void processNoticeGenerationShouldRollbackToProcessingWhenCompletionEventCannotBePublished() {

        doReturn(templateFile).when(noticeTemplateStorageClient).getTemplate(any());

        doReturn(CreditorInstitution.builder().webChannel(true).physicalChannel("Test").fullName("Test").logo("logo")
                .cbill("Cbill").organization("ORG").build()).when(institutionsStorageClient).getInstitutionData(any());

        doReturn(getPdfEngineResponse(HttpStatus.SC_OK, noticeFile.getPath())).when(pdfEngineClient).generatePDF(any(),
                any());

        doReturn(true).when(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());

        doReturn(1L).when(paymentGenerationRequestRepository).findAndAddItemById(any(), any());

        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().id("test")
                .status(PaymentGenerationRequestStatus.PROCESSING).numberOfElementsTotal(1).numberOfElementsFailed(0)
                .items(Collections.singletonList("test")).build())).when(paymentGenerationRequestRepository)
                .findById(any());

        doReturn(1L).when(paymentGenerationRequestRepository).findAndSetToComplete(any());

        /*
         * Simulate an Event Hub publication failure after Mongo has already been moved
         * to COMPLETING.
         */
        doReturn(false).when(noticeRequestCompleteProducer).noticeComplete(any());

        /*
         * Simulate a successful compensation from COMPLETING back to PROCESSING, so
         * that the generation message can be retried.
         */
        doReturn(1L).when(paymentGenerationRequestRepository).findAndSetToProcessing(any());

        NoticeRequestEH noticeRequestEH = NoticeRequestEH.builder().folderId("test")
                .noticeData(
                        NoticeGenerationRequestItem.builder().templateId("template")
                                .data(NoticeRequestData.builder().notice(Notice.builder().code("code")
                                        .dueDate("24/10/2024").subject("subject").paymentAmount(100L)
                                        .reduced(InstallmentData.builder().amount(100L).code("codeRate")
                                                .dueDate("24/10/2024").build())
                                        .discounted(InstallmentData.builder().amount(100L).code("codeRate")
                                                .dueDate("24/10/2024").build())
                                        .installments(Collections.singletonList(InstallmentData.builder().amount(100L)
                                                .code("codeRate").dueDate("24/10/2024").build()))
                                        .build())
                                        .creditorInstitution(CreditorInstitution.builder().taxCode("taxCode").build())
                                        .debtor(Debtor.builder().taxCode("taxCode").address("address").city("city")
                                                .buildingNumber("101").postalCode("00135").province("RM")
                                                .fullName("Test Name").build())
                                        .build())
                                .build())
                .build();

        /*
         * The failure concerns only the publication of the completion event. The PDF
         * has already been successfully generated and stored, therefore it must not be
         * converted into a notice generation error.
         */
        String message = objectMapper.writeValueAsString(noticeRequestEH);
        assertThrows(CompletionEventPublicationException.class,
                () -> noticeGenerationService.processNoticeGenerationEH(message));

        /*
         * MDC context must also be cleared when processing terminates with an
         * exception, otherwise the reused Kafka consumer thread could leak this
         * folder/item context.
         */
        assertNull(MDC.get("folderId"));
        assertNull(MDC.get("itemId"));

        /*
         * The notice itself must have been generated and stored successfully.
         */
        verify(pdfEngineClient).generatePDF(any(), any());

        verify(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());

        verify(paymentGenerationRequestRepository).findAndAddItemById(any(), any());

        /*
         * Once every notice has been processed, this instance acquires the PROCESSING
         * -> COMPLETING transition.
         */
        verify(paymentGenerationRequestRepository).findAndSetToComplete("test");

        /*
         * The completion event publication is attempted with a folder already marked as
         * COMPLETING.
         */
        verify(noticeRequestCompleteProducer).noticeComplete(
                argThat(request -> PaymentGenerationRequestStatus.COMPLETING.equals(request.getStatus())));

        /*
         * Since the completion event could not be published, the folder must be
         * restored to PROCESSING instead of remaining permanently stuck in COMPLETING.
         */
        verify(paymentGenerationRequestRepository).findAndSetToProcessing("test");

        /*
         * A completion publication failure is not a notice generation failure: the PDF
         * was successfully produced and stored, therefore no error record must be
         * created and the failed notices counter must not be incremented.
         */
        verifyNoInteractions(paymentGenerationRequestErrorRepository);

        verify(paymentGenerationRequestRepository, never()).findAndIncrementNumberOfElementsFailedById(any());

        verifyNoInteractions(noticeRequestErrorProducer);
    }

    @SneakyThrows
    @Test
    void processNoticeGenerationShouldRollbackToProcessingWhenCompletionEventPublicationThrowsException() {

        doReturn(templateFile).when(noticeTemplateStorageClient).getTemplate(any());

        doReturn(CreditorInstitution.builder().webChannel(true).physicalChannel("Test").fullName("Test").logo("logo")
                .cbill("Cbill").organization("ORG").build()).when(institutionsStorageClient).getInstitutionData(any());

        doReturn(getPdfEngineResponse(HttpStatus.SC_OK, noticeFile.getPath())).when(pdfEngineClient).generatePDF(any(),
                any());

        doReturn(true).when(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());

        doReturn(1L).when(paymentGenerationRequestRepository).findAndAddItemById(any(), any());

        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().id("test")
                .status(PaymentGenerationRequestStatus.PROCESSING).numberOfElementsTotal(1).numberOfElementsFailed(0)
                .items(Collections.singletonList("test")).build())).when(paymentGenerationRequestRepository)
                .findById(any());

        /*
         * Simulate transition from PROCESSING to COMPLETING.
         */
        doReturn(1L).when(paymentGenerationRequestRepository).findAndSetToComplete(any());

        /*
         * Simulate an unexpected Event Hub/binder failure while publishing the
         * completion event.
         */
        doThrow(new RuntimeException("Event Hub unavailable")).when(noticeRequestCompleteProducer)
                .noticeComplete(any());

        /*
         * The compensation succeeds and makes the folder eligible for another
         * completion attempt.
         */
        doReturn(1L).when(paymentGenerationRequestRepository).findAndSetToProcessing(any());

        NoticeRequestEH noticeRequestEH = NoticeRequestEH.builder().folderId("test")
                .noticeData(
                        NoticeGenerationRequestItem.builder().templateId("template")
                                .data(NoticeRequestData.builder().notice(Notice.builder().code("code")
                                        .dueDate("24/10/2024").subject("subject").paymentAmount(100L)
                                        .reduced(InstallmentData.builder().amount(100L).code("codeRate")
                                                .dueDate("24/10/2024").build())
                                        .discounted(InstallmentData.builder().amount(100L).code("codeRate")
                                                .dueDate("24/10/2024").build())
                                        .installments(Collections.singletonList(InstallmentData.builder().amount(100L)
                                                .code("codeRate").dueDate("24/10/2024").build()))
                                        .build())
                                        .creditorInstitution(CreditorInstitution.builder().taxCode("taxCode").build())
                                        .debtor(Debtor.builder().taxCode("taxCode").address("address").city("city")
                                                .buildingNumber("101").postalCode("00135").province("RM")
                                                .fullName("Test Name").build())
                                        .build())
                                .build())
                .build();

        String message = objectMapper.writeValueAsString(noticeRequestEH);
        assertThrows(CompletionEventPublicationException.class,
                () -> noticeGenerationService.processNoticeGenerationEH(message));

        verify(pdfEngineClient).generatePDF(any(), any());

        verify(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());

        verify(paymentGenerationRequestRepository).findAndAddItemById(any(), any());

        /*
         * The folder was first moved from PROCESSING to COMPLETING.
         */
        verify(paymentGenerationRequestRepository).findAndSetToComplete("test");

        /*
         * Publication of the completion event was attempted exactly once.
         */
        verify(noticeRequestCompleteProducer).noticeComplete(any());

        /*
         * Since the publication threw an exception, the COMPLETING transition must be
         * roll backed.
         */
        verify(paymentGenerationRequestRepository).findAndSetToProcessing("test");

        verifyNoInteractions(paymentGenerationRequestErrorRepository);

        verify(paymentGenerationRequestRepository, never()).findAndIncrementNumberOfElementsFailedById(any());

        verifyNoInteractions(noticeRequestErrorProducer);
    }

    @SneakyThrows
    @Test
    void processNoticeGenerationShouldPropagateCompletionFailureWhenRollbackFails() {

        doReturn(templateFile).when(noticeTemplateStorageClient).getTemplate(any());

        doReturn(CreditorInstitution.builder().webChannel(true).physicalChannel("Test").fullName("Test").logo("logo")
                .cbill("Cbill").organization("ORG").build()).when(institutionsStorageClient).getInstitutionData(any());

        doReturn(getPdfEngineResponse(HttpStatus.SC_OK, noticeFile.getPath())).when(pdfEngineClient).generatePDF(any(),
                any());

        doReturn(true).when(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());

        doReturn(1L).when(paymentGenerationRequestRepository).findAndAddItemById(any(), any());

        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().id("test")
                .status(PaymentGenerationRequestStatus.PROCESSING).numberOfElementsTotal(1).numberOfElementsFailed(0)
                .items(Collections.singletonList("test")).build())).when(paymentGenerationRequestRepository)
                .findById(any());

        // The current consumer successfully acquires the PROCESSING -> COMPLETING transition.
        doReturn(1L).when(paymentGenerationRequestRepository).findAndSetToComplete(any());

        /*
         * Simulate a completion event publication failure after Mongo has already been
         * moved to COMPLETING.
         */
        doReturn(false).when(noticeRequestCompleteProducer).noticeComplete(any());

        /*
         * Simulate a second failure while trying to compensate the transition from
         * COMPLETING back to PROCESSING.
         */
        doThrow(new RuntimeException("Mongo unavailable")).when(paymentGenerationRequestRepository)
                .findAndSetToProcessing("test");

        NoticeRequestEH noticeRequestEH = NoticeRequestEH.builder().folderId("test")
                .noticeData(
                        NoticeGenerationRequestItem.builder().templateId("template")
                                .data(NoticeRequestData.builder().notice(Notice.builder().code("code")
                                        .dueDate("24/10/2024").subject("subject").paymentAmount(100L)
                                        .reduced(InstallmentData.builder().amount(100L).code("codeRate")
                                                .dueDate("24/10/2024").build())
                                        .discounted(InstallmentData.builder().amount(100L).code("codeRate")
                                                .dueDate("24/10/2024").build())
                                        .installments(Collections.singletonList(InstallmentData.builder().amount(100L)
                                                .code("codeRate").dueDate("24/10/2024").build()))
                                        .build())
                                        .creditorInstitution(CreditorInstitution.builder().taxCode("taxCode").build())
                                        .debtor(Debtor.builder().taxCode("taxCode").address("address").city("city")
                                                .buildingNumber("101").postalCode("00135").province("RM")
                                                .fullName("Test Name").build())
                                        .build())
                                .build())
                .build();

        String message = objectMapper.writeValueAsString(noticeRequestEH);

        // The rollback failure must not replace the original publication failure.
        CompletionEventPublicationException exception = assertThrows(CompletionEventPublicationException.class,
                () -> noticeGenerationService.processNoticeGenerationEH(message));

        // The rollback failure is retained for diagnostics as a suppressed exception.
        org.junit.jupiter.api.Assertions.assertEquals(1, exception.getSuppressed().length);

        org.junit.jupiter.api.Assertions.assertEquals("Mongo unavailable", exception.getSuppressed()[0].getMessage());

        // The notice itself was successfully generated and stored.
        verify(pdfEngineClient).generatePDF(any(), any());
        verify(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());
        verify(paymentGenerationRequestRepository).findAndAddItemById(any(), any());

        // The folder first reached COMPLETING.
        verify(paymentGenerationRequestRepository).findAndSetToComplete("test");

        // Completion publication was attempted.
        verify(noticeRequestCompleteProducer).noticeComplete(any());

        // Compensation was attempted even though Mongo failed.
        verify(paymentGenerationRequestRepository).findAndSetToProcessing("test");

        /*
         * A completion publication failure must never be converted into a notice
         * generation failure because the PDF was already generated successfully.
         */
        verifyNoInteractions(paymentGenerationRequestErrorRepository);
        verify(paymentGenerationRequestRepository, never()).findAndIncrementNumberOfElementsFailedById(any());
        verifyNoInteractions(noticeRequestErrorProducer);
    }
    
    @SneakyThrows
    @Test
    void processNoticeGenerationShouldPropagateCompletionFailureWhenRollbackUpdatesNoRequest() {

        doReturn(templateFile).when(noticeTemplateStorageClient).getTemplate(any());

        doReturn(CreditorInstitution.builder().webChannel(true).physicalChannel("Test").fullName("Test").logo("logo")
                .cbill("Cbill").organization("ORG").build()).when(institutionsStorageClient).getInstitutionData(any());

        doReturn(getPdfEngineResponse(HttpStatus.SC_OK, noticeFile.getPath())).when(pdfEngineClient).generatePDF(any(),
                any());

        doReturn(true).when(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());

        doReturn(1L).when(paymentGenerationRequestRepository).findAndAddItemById(any(), any());

        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().id("test")
                .status(PaymentGenerationRequestStatus.PROCESSING).numberOfElementsTotal(1).numberOfElementsFailed(0)
                .items(Collections.singletonList("test")).build())).when(paymentGenerationRequestRepository)
                .findById(any());

        doReturn(1L).when(paymentGenerationRequestRepository).findAndSetToComplete(any());

        /*
         * Simulate a completion event publication failure after the folder has already
         * been moved to COMPLETING.
         */
        doReturn(false).when(noticeRequestCompleteProducer).noticeComplete(any());

        /*
         * Simulate a compensation attempt that does not update any document, for
         * example because the folder is no longer in COMPLETING status.
         */
        doReturn(0L).when(paymentGenerationRequestRepository).findAndSetToProcessing("test");

        NoticeRequestEH noticeRequestEH = NoticeRequestEH.builder().folderId("test")
                .noticeData(NoticeGenerationRequestItem.builder().templateId("template").data(NoticeRequestData
                        .builder()
                        .notice(Notice.builder().code("code").dueDate("24/10/2024").subject("subject")
                                .paymentAmount(100L).build())
                        .creditorInstitution(CreditorInstitution.builder().taxCode("taxCode").build())
                        .debtor(Debtor.builder().taxCode("taxCode").address("address").city("city")
                                .buildingNumber("101").postalCode("00135").province("RM").fullName("Test Name").build())
                        .build()).build())
                .build();

        String message = objectMapper.writeValueAsString(noticeRequestEH);

        assertThrows(CompletionEventPublicationException.class,
                () -> noticeGenerationService.processNoticeGenerationEH(message));

        /*
         * Even if compensation does not update a document, the original completion
         * publication failure must still be propagated and no notice failure created.
         */
        verify(paymentGenerationRequestRepository).findAndSetToComplete("test");
        verify(noticeRequestCompleteProducer).noticeComplete(any());
        verify(paymentGenerationRequestRepository).findAndSetToProcessing("test");

        verifyNoInteractions(paymentGenerationRequestErrorRepository);
        verify(paymentGenerationRequestRepository, never()).findAndIncrementNumberOfElementsFailedById(any());

        assertNull(MDC.get("folderId"));
        assertNull(MDC.get("itemId"));
    }

    private PdfEngineResponse getPdfEngineResponse(int status, String pdfPath) {
        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
        pdfEngineResponse.setTempPdfPath(pdfPath);
        if (status != HttpStatus.SC_OK) {
            pdfEngineResponse.setErrorMessage("error");
        }
        pdfEngineResponse.setStatusCode(status);
        return pdfEngineResponse;
    }
}
