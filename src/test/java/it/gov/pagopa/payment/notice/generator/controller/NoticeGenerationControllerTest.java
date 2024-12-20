package it.gov.pagopa.payment.notice.generator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.notice.generator.model.NoticeGenerationRequestItem;
import it.gov.pagopa.payment.notice.generator.model.notice.*;
import it.gov.pagopa.payment.notice.generator.service.NoticeGenerationServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NoticeGenerationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private NoticeGenerationServiceImpl noticeGenerationService;

    @Autowired
    private ObjectMapper objectMapper;

    private static NoticeGenerationRequestItem getNoticeGenerationRequestItem() {
        return NoticeGenerationRequestItem.builder()
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
                .build();
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(noticeGenerationService);
    }

    @Test
    void generateNoticeShouldReturnFileOnOk() throws Exception {
        File tempDirectory = Files.createTempDirectory("test").toFile();
        File file = Files.createTempFile(tempDirectory.toPath(), "test", ".zip").toFile();
        when(noticeGenerationService.generateNotice(any(), any(), any()))
                .thenReturn(file);
        String url = "/notices/generate";
        mvc.perform(post(url)
                        .param("folderId", "test")
                        .content(objectMapper.writeValueAsString(
                                getNoticeGenerationRequestItem()))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
        verify(noticeGenerationService).generateNotice(any(), any(), any());
    }


    @Test
    void getNoticeCode(){
        NoticeGenerationRequestItem noticeGenerationRequestItem = NoticeGenerationRequestItem.builder()
                .data(NoticeRequestData.builder()
                        .notice(Notice.builder()
                                .code("123")
                                .build())
                        .build())
                .build();
        var code = NoticeGenerationServiceImpl.getNoticeCode(noticeGenerationRequestItem);
        Assertions.assertEquals("123", code);
    }

    @Test
    void getNoticeCodeReduced(){
        NoticeGenerationRequestItem noticeGenerationRequestItem = NoticeGenerationRequestItem.builder()
                .data(NoticeRequestData.builder()
                        .notice(Notice.builder()
                                .reduced(InstallmentData.builder()
                                        .code("123")
                                        .build())
                                .build())
                        .build())
                .build();
        var code = NoticeGenerationServiceImpl.getNoticeCode(noticeGenerationRequestItem);
        Assertions.assertEquals("123", code);
    }

    @Test
    void getNoticeCodeDiscounted(){
        NoticeGenerationRequestItem noticeGenerationRequestItem = NoticeGenerationRequestItem.builder()
                .data(NoticeRequestData.builder()
                        .notice(Notice.builder()
                                .discounted(InstallmentData.builder()
                                        .code("123")
                                        .build())
                                .build())
                        .build())
                .build();
        var code = NoticeGenerationServiceImpl.getNoticeCode(noticeGenerationRequestItem);
        Assertions.assertEquals("123", code);
    }

    @Test
    void getNoticeCodeInstalment(){
        NoticeGenerationRequestItem noticeGenerationRequestItem = NoticeGenerationRequestItem.builder()
                .data(NoticeRequestData.builder()
                        .notice(Notice.builder()
                                .installments(List.of(InstallmentData.builder()
                                                .code("123")
                                        .build()))
                                .build())
                        .build())
                .build();
        var code = NoticeGenerationServiceImpl.getNoticeCode(noticeGenerationRequestItem);
        Assertions.assertEquals("123", code);
    }

    @Test
    void getNoticeCodeMissing(){
        NoticeGenerationRequestItem noticeGenerationRequestItem = NoticeGenerationRequestItem.builder()
                .data(NoticeRequestData.builder()
                        .notice(Notice.builder()
                                .build())
                        .build())
                .build();
        var code = NoticeGenerationServiceImpl.getNoticeCode(noticeGenerationRequestItem);
        Assertions.assertNotNull(code);
    }


}
