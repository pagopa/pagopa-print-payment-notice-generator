package it.gov.pagopa.payment.notice.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.notice.generator.client.PdfEngineClient;
import it.gov.pagopa.payment.notice.generator.entity.PaymentNoticeGenerationRequest;
import it.gov.pagopa.payment.notice.generator.exception.AppException;
import it.gov.pagopa.payment.notice.generator.model.NoticeGenerationRequestItem;
import it.gov.pagopa.payment.notice.generator.model.NoticeRequestEH;
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
        noticeGenerationService = new NoticeGenerationServiceImpl(
                paymentGenerationRequestRepository, paymentGenerationRequestErrorRepository,
                institutionsStorageClient, noticeStorageClient, noticeTemplateStorageClient,
                pdfEngineClient, new Aes256Utils("test","test"), objectMapper,
                validator);
    }

    @SneakyThrows
    @Test
    void processNoticeGenerationShouldReturnOkOnValidData() {

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
                    .build()
        ).when(institutionsStorageClient).getInstitutionData(any());
        doReturn(getPdfEngineResponse(HttpStatus.SC_OK, noticeFile.getPath()))
                .when(pdfEngineClient).generatePDF(any(), any());
        doReturn(true).when(noticeStorageClient).savePdfToBlobStorage(any(), any(), any());
        doReturn(1L).when(paymentGenerationRequestRepository).findAndAddItemById(any(),any());

        NoticeRequestEH noticeRequestEH = NoticeRequestEH
                .builder()
                .folderId("test")
                .noticeGenerationRequestItem(NoticeGenerationRequestItem.builder()
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
        noticeGenerationService.processNoticeGenerationEH(objectMapper.writeValueAsString(noticeRequestEH));
        verify(paymentGenerationRequestRepository).findById(any());
        verify(paymentGenerationRequestRepository).findAndAddItemById(any(), any());
        verify(noticeStorageClient).savePdfToBlobStorage(any(),any(),any());
        verify(institutionsStorageClient).getInstitutionData(any());
        verify(noticeTemplateStorageClient).getTemplate(any());
        verify(pdfEngineClient).generatePDF(any(),any());
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
                .build()
        ).when(institutionsStorageClient).getInstitutionData(any());
        doReturn(getPdfEngineResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, noticeFile.getPath()))
                .when(pdfEngineClient).generatePDF(any(), any());

        NoticeRequestEH noticeRequestEH = NoticeRequestEH
                .builder()
                .folderId("test")
                .noticeGenerationRequestItem(NoticeGenerationRequestItem.builder()
                        .templateId("template")
                        .data(NoticeRequestData.builder()
                                .notice(Notice.builder()
                                        .code("code")
                                        .dueDate("24/10/2024")
                                        .subject("subject")
                                        .paymentAmount(100L)
                                        .posteDocumentType("0121")
                                        .posteAuth("0232323")
                                        .installments(Collections.singletonList(
                                                InstallmentData.builder()
                                                        .amount(100L)
                                                        .code("codeRate")
                                                        .dueDate("24/10/2024")
                                                        .posteAuth("02323")
                                                        .posteAuth("322323")
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
        verify(pdfEngineClient).generatePDF(any(),any());
        verify(paymentGenerationRequestErrorRepository).save(any());
        verify(paymentGenerationRequestRepository).findAndIncrementNumberOfElementsFailedById(any());
        verifyNoInteractions(noticeStorageClient);
    }

    @SneakyThrows
    @Test
    void processNoticeGenerationShouldReturnKoOnInvalidData() {

        NoticeRequestEH noticeRequestEH = NoticeRequestEH
                .builder()
                .noticeGenerationRequestItem(NoticeGenerationRequestItem.builder()
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
        noticeGenerationService.processNoticeGenerationEH(objectMapper.writeValueAsString(noticeRequestEH));
        verify(paymentGenerationRequestErrorRepository).save(any());
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
