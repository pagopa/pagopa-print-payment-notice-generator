package it.gov.pagopa.payment.notice.generator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.notice.generator.model.NoticeGenerationRequestItem;
import it.gov.pagopa.payment.notice.generator.model.notice.*;
import it.gov.pagopa.payment.notice.generator.service.NoticeGenerationServiceImpl;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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

    @BeforeEach
    void setUp() {
        Mockito.reset(noticeGenerationService);
    }

    @Test
    void generateNoticeShouldReturnFileOnOk() throws Exception {
        File tempDirectory = Files.createTempDirectory("test").toFile();
        File file = Files.createTempFile(tempDirectory.toPath(), "test", ".zip").toFile();
        when(noticeGenerationService.generateNotice(any(),any(), any()))
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
        verify(noticeGenerationService).generateNotice(any(),any(), any());
    }

    @Test
    void generateNoticeShouldReturnKOonErrorFile() throws Exception {
        when(noticeGenerationService.generateNotice(any(),any(), any()))
                .thenReturn(null);
        String url = "/notices/generate";
        mvc.perform(post(url)
                        .param("folderId", "test")
                        .content(objectMapper.writeValueAsString(
                                getNoticeGenerationRequestItem()))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is5xxServerError());
        verify(noticeGenerationService).generateNotice(any(),any(), any());
    }

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


}
