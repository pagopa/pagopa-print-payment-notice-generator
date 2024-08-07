package it.gov.pagopa.payment.notice.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.notice.generator.client.PdfEngineClient;
import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.payment.notice.generator.events.producer.NoticeRequestCompleteProducer;
import it.gov.pagopa.payment.notice.generator.events.producer.NoticeRequestErrorProducer;
import it.gov.pagopa.payment.notice.generator.exception.AppException;
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
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeGenerationServiceImplTest {

    @Mock
    public PaymentGenerationRequestRepository paymentGenerationRequestRepository;

    @Mock
    public PaymentGenerationRequestErrorRepository paymentGenerationRequestErrorRepository;

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
    public NoticeGenerationServiceImplTest() {
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
    public void init() {
        Mockito.reset(paymentGenerationRequestErrorRepository, paymentGenerationRequestRepository,
                institutionsStorageClient, noticeStorageClient, noticeTemplateStorageClient, pdfEngineClient);
        lenient().when(noticeTemplateStorageClient.getTemplates()).thenReturn(Collections.emptyList());
        noticeGenerationService = new NoticeGenerationServiceImpl(
                paymentGenerationRequestRepository, paymentGenerationRequestErrorRepository,
                institutionsStorageClient, noticeStorageClient, noticeTemplateStorageClient,
                pdfEngineClient, new Aes256Utils("test", "test"), objectMapper,
                validator, noticeRequestCompleteProducer, noticeRequestErrorProducer);
    }

    @SneakyThrows
    @Test
    void processNoticeGenerationShouldReturnOkOnValidData() {

        doReturn(templateFile).when(noticeTemplateStorageClient).getTemplate(any());
        doReturn(CreditorInstitution.builder()
                .webChannel(true)
                .physicalChannel("Test")
                .fullName("Test")
                .logo("logo")
                .cbill("Cbill")
                .organization("ORG")
                .build()
        ).when(institutionsStorageClient).getInstitutionData(any());
        doReturn(getPdfEngineResponse(HttpStatus.SC_OK, noticeFile.getPath()))
                .when(pdfEngineClient).generatePDF(any(), any());
        doReturn(true).when(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());
        doReturn(1L).when(paymentGenerationRequestRepository).findAndAddItemById(any(), any());
        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().status(PaymentGenerationRequestStatus.PROCESSING)
                .numberOfElementsTotal(1).numberOfElementsFailed(0)
                .items(Collections.singletonList("test")).build())).when(paymentGenerationRequestRepository)
                .findById(any());
        doReturn(1L).when(paymentGenerationRequestRepository).findAndSetToComplete(any());

        NoticeRequestEH noticeRequestEH = NoticeRequestEH
                .builder()
                .folderId("test")
                .noticeData(NoticeGenerationRequestItem.builder()
                        .templateId("template")
                        .data(NoticeRequestData.builder()
                                .notice(Notice.builder()
                                        .code("code")
                                        .dueDate("24/10/2024")
                                        .subject("subject")
                                        .paymentAmount(100L)
                                        .reduced(
                                                InstallmentData.builder()
                                                        .amount(100L)
                                                        .code("codeRate")
                                                        .dueDate("24/10/2024")
                                                        .build())
                                        .discounted(
                                                InstallmentData.builder()
                                                        .amount(100L)
                                                        .code("codeRate")
                                                        .dueDate("24/10/2024")
                                                        .build())
                                        .installments(Collections.singletonList(
                                                InstallmentData.builder()
                                                        .amount(100L)
                                                        .code("codeRate")
                                                        .dueDate("24/10/2024")
                                                        .build()
                                        ))
                                        .build())
                                .creditorInstitution(CreditorInstitution.builder()
                                        .taxCode("taxCode")
                                        .build())
                                .debtor(Debtor.builder()
                                        .taxCode("taxCode")
                                        .address("address")
                                        .city("city")
                                        .buildingNumber("101")
                                        .postalCode("00135")
                                        .province("RM")
                                        .fullName("Test Name")
                                        .build())
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
    void processNoticeGenerationShouldReturnKOOnPDfEngineBadRequest() {

        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().build()))
                .when(paymentGenerationRequestRepository).findById(any());
        doReturn(templateFile).when(noticeTemplateStorageClient).getTemplate(any());
        doReturn(CreditorInstitution.builder()
                .webChannel(true)
                .physicalChannel("Test")
                .fullName("Test")
                .logo("logo")
                .cbill("Cbill")
                .organization("ORG")
                .posteAccountNumber("131213")
                .posteAuth("322323")
                .build()
        ).when(institutionsStorageClient).getInstitutionData(any());
        doReturn(getPdfEngineResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, noticeFile.getPath()))
                .when(pdfEngineClient).generatePDF(any(), any());

        NoticeRequestEH noticeRequestEH = NoticeRequestEH
                .builder()
                .folderId("test")
                .noticeData(NoticeGenerationRequestItem.builder()
                        .templateId("template")
                        .data(NoticeRequestData.builder()
                                .notice(Notice.builder()
                                        .code("code")
                                        .dueDate("24/10/2024")
                                        .subject("subject")
                                        .paymentAmount(100L)
                                        .installments(Collections.singletonList(
                                                InstallmentData.builder()
                                                        .amount(100L)
                                                        .code("codeRate")
                                                        .dueDate("24/10/2024")
                                                        .build()
                                        ))
                                        .build())
                                .creditorInstitution(CreditorInstitution.builder()
                                        .taxCode("taxCode")
                                        .build())
                                .debtor(Debtor.builder()
                                        .taxCode("taxCode")
                                        .address("address")
                                        .city("city")
                                        .buildingNumber("101")
                                        .postalCode("00135")
                                        .province("RM")
                                        .fullName("Test Name")
                                        .build())
                                .build())
                        .build())
                .build();
        Assert.assertThrows(AppException.class, () ->
                noticeGenerationService.processNoticeGenerationEH(
                        objectMapper.writeValueAsString(noticeRequestEH)));
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

        NoticeRequestEH noticeRequestEH = NoticeRequestEH
                .builder()
                .noticeData(NoticeGenerationRequestItem.builder()
                        .templateId("template")
                        .data(NoticeRequestData.builder()
                                .notice(Notice.builder()
                                        .code("code")
                                        .dueDate("24/10/2024")
                                        .subject("subject")
                                        .paymentAmount(100L)
                                        .installments(Collections.singletonList(
                                                InstallmentData.builder()
                                                        .amount(100L)
                                                        .code("codeRate")
                                                        .dueDate("24/10/2024")
                                                        .build()
                                        ))
                                        .build())
                                .creditorInstitution(CreditorInstitution.builder()
                                        .taxCode("taxCode")
                                        .build())
                                .debtor(Debtor.builder()
                                        .taxCode("taxCode")
                                        .address("address")
                                        .city("city")
                                        .buildingNumber("101")
                                        .postalCode("00135")
                                        .province("RM")
                                        .fullName("Test Name")
                                        .build())
                                .build())
                        .build())
                .build();
        Assert.assertThrows(AppException.class, () ->
                noticeGenerationService.processNoticeGenerationEH(
                        objectMapper.writeValueAsString(noticeRequestEH)));
    }

    @SneakyThrows
    @Test
    void processNoticeGenerationShouldReturnKOOnPDfEngineBadRequestWithRepoException() {

        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().build()))
                .when(paymentGenerationRequestRepository).findById(any());
        doAnswer(item -> {
            throw new Exception("Could not increment data");
        }).when(paymentGenerationRequestRepository).findAndIncrementNumberOfElementsFailedById(any());
        doReturn(templateFile).when(noticeTemplateStorageClient).getTemplate(any());
        doReturn(Optional.empty()).when(paymentGenerationRequestErrorRepository).findByErrorIdAndFolderId(any(), any());
        doReturn(CreditorInstitution.builder()
                .webChannel(true)
                .physicalChannel("Test")
                .fullName("Test")
                .logo("logo")
                .cbill("Cbill")
                .organization("ORG")
                .posteAccountNumber("131213")
                .posteAuth("322323")
                .build()
        ).when(institutionsStorageClient).getInstitutionData(any());
        doReturn(getPdfEngineResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, noticeFile.getPath()))
                .when(pdfEngineClient).generatePDF(any(), any());

        NoticeRequestEH noticeRequestEH = NoticeRequestEH
                .builder()
                .folderId("test")
                .noticeData(NoticeGenerationRequestItem.builder()
                        .templateId("template")
                        .data(NoticeRequestData.builder()
                                .notice(Notice.builder()
                                        .code("code")
                                        .dueDate("24/10/2024")
                                        .subject("subject")
                                        .paymentAmount(100L)
                                        .installments(Collections.singletonList(
                                                InstallmentData.builder()
                                                        .amount(100L)
                                                        .code("codeRate")
                                                        .dueDate("24/10/2024")
                                                        .build()
                                        ))
                                        .build())
                                .creditorInstitution(CreditorInstitution.builder()
                                        .taxCode("taxCode")
                                        .build())
                                .debtor(Debtor.builder()
                                        .taxCode("taxCode")
                                        .address("address")
                                        .city("city")
                                        .buildingNumber("101")
                                        .postalCode("00135")
                                        .province("RM")
                                        .fullName("Test Name")
                                        .build())
                                .build())
                        .build())
                .build();
        Assert.assertThrows(AppException.class, () ->
                noticeGenerationService.processNoticeGenerationEH(
                        objectMapper.writeValueAsString(noticeRequestEH)));
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

        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().build()))
                .when(paymentGenerationRequestRepository).findById(any());
        doReturn(CreditorInstitution.builder()
                .webChannel(true)
                .physicalChannel("Test")
                .fullName("Test")
                .logo("logo")
                .cbill("Cbill")
                .organization("ORG")
                .posteAccountNumber("131213")
                .posteAuth("322323")
                .build()
        ).when(institutionsStorageClient).getInstitutionData(any());
        when(noticeTemplateStorageClient.getTemplates()).thenReturn(Collections.singletonList(
                TemplateResource.builder().templateId("template").templateValidationRules(
                        "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"title\":\"Default notice validation schema\"," +
                                "\"description\":\"Default validation schema\",\"required\":[\"extra\",\"debtor\",\"payee\",\"notice\"]," +
                                "\"properties\":{\"debtor\":{\"type\":\"object\"},\"payee\":{\"type\":\"object\"}," +
                                "\"notice\":{\"type\":\"object\",\"required\":[\"qrCode\"]," +
                                "\"properties\":{\"qrCode\":{\"type\":\"string\"}}}}}").build()
        ));

        NoticeRequestEH noticeRequestEH = NoticeRequestEH
                .builder()
                .folderId("test")
                .noticeData(NoticeGenerationRequestItem.builder()
                        .templateId("template")
                        .data(NoticeRequestData.builder()
                                .notice(Notice.builder()
                                        .code("code")
                                        .dueDate("24/10/2024")
                                        .subject("subject")
                                        .paymentAmount(100L)
                                        .installments(Collections.singletonList(
                                                InstallmentData.builder()
                                                        .amount(100L)
                                                        .code("codeRate")
                                                        .dueDate("24/10/2024")
                                                        .build()
                                        ))
                                        .build())
                                .creditorInstitution(CreditorInstitution.builder()
                                        .taxCode("taxCode")
                                        .build())
                                .debtor(Debtor.builder()
                                        .taxCode("taxCode")
                                        .address("address")
                                        .city("city")
                                        .buildingNumber("101")
                                        .postalCode("00135")
                                        .province("RM")
                                        .fullName("Test Name")
                                        .build())
                                .build())
                        .build())
                .build();
        Assert.assertThrows(AppException.class, () ->
                noticeGenerationService.processNoticeGenerationEH(
                        objectMapper.writeValueAsString(noticeRequestEH)));
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
        when(noticeTemplateStorageClient.getTemplates()).thenReturn(Collections.singletonList(
                TemplateResource.builder().templateId("template").templateValidationRules(
                        "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"title\":\"Default notice validation schema\"," +
                                "\"description\":\"Default validation schema\",\"required\":[\"debtor\",\"creditorInstitution\",\"notice\"]," +
                                "\"properties\":{\"debtor\":{\"type\":\"object\"},\"creditorInstitution\":{\"type\":\"object\"}," +
                                "\"notice\":{\"type\":\"object\"" +
                                "}}}}}").build()
        ));
        doReturn(CreditorInstitution.builder()
                .webChannel(true)
                .physicalChannel("Test")
                .fullName("Test")
                .logo("logo")
                .cbill("Cbill")
                .organization("ORG")
                .build()
        ).when(institutionsStorageClient).getInstitutionData(any());
        doReturn(getPdfEngineResponse(HttpStatus.SC_OK, noticeFile.getPath()))
                .when(pdfEngineClient).generatePDF(any(), any());
        doReturn(true).when(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());
        doReturn(1L).when(paymentGenerationRequestRepository).findAndAddItemById(any(), any());
        doReturn(Optional.of(PaymentNoticeGenerationRequest.builder().status(PaymentGenerationRequestStatus.PROCESSING)
                .numberOfElementsTotal(1).numberOfElementsFailed(0)
                .items(Collections.singletonList("test")).build())).when(paymentGenerationRequestRepository)
                .findById(any());
        doReturn(1L).when(paymentGenerationRequestRepository).findAndSetToComplete(any());

        NoticeRequestEH noticeRequestEH = NoticeRequestEH
                .builder()
                .folderId("test")
                .noticeData(NoticeGenerationRequestItem.builder()
                        .templateId("template")
                        .data(NoticeRequestData.builder()
                                .notice(Notice.builder()
                                        .code("code")
                                        .dueDate("24/10/2024")
                                        .subject("subject")
                                        .paymentAmount(100L)
                                        .reduced(InstallmentData.builder()
                                                .amount(100L)
                                                .code("codeRate")
                                                .dueDate("24/10/2024")
                                                .build())
                                        .discounted(InstallmentData.builder()
                                                .amount(100L)
                                                .code("codeRate")
                                                .dueDate("24/10/2024")
                                                .build())
                                        .installments(Collections.singletonList(
                                                InstallmentData.builder()
                                                        .amount(100L)
                                                        .code("codeRate")
                                                        .dueDate("24/10/2024")
                                                        .build()
                                        ))
                                        .build())
                                .creditorInstitution(CreditorInstitution.builder()
                                        .taxCode("taxCode")
                                        .build())
                                .debtor(Debtor.builder()
                                        .taxCode("taxCode")
                                        .address("address")
                                        .city("city")
                                        .buildingNumber("101")
                                        .postalCode("00135")
                                        .province("RM")
                                        .fullName("Test Name")
                                        .build())
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
