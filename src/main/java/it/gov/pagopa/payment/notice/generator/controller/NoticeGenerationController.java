package it.gov.pagopa.payment.notice.generator.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.payment.notice.generator.model.NoticeGenerationRequestItem;
import it.gov.pagopa.payment.notice.generator.service.NoticeGenerationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@RestController
@RequestMapping(value = "/notices", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Slf4j
@Tag(name = "Notice Generation APIs")
public class NoticeGenerationController {

    private final NoticeGenerationService noticeGenerationService;

    public NoticeGenerationController(NoticeGenerationService noticeGenerationService) {
        this.noticeGenerationService = noticeGenerationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<Resource> generateNotice(
            @Valid @NotNull @RequestParam("folderId") String folderId,
            @Parameter(description = "templateId to use for retrieval")
            @Valid @NotNull @RequestBody NoticeGenerationRequestItem noticeGenerationRequestItem) {
        File file = noticeGenerationService.generateNotice(noticeGenerationRequestItem, folderId);
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + ".pdf\"");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .headers(headers)
                    .body(new ByteArrayResource(inputStream.readAllBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            clearTempDirectory(file.toPath().getParent());
        }
    }

    private void clearTempDirectory(java.nio.file.Path workingDirPath) {
        try {
            FileUtils.deleteDirectory(workingDirPath.toFile());
        } catch (IOException e) {
            log.warn("Unable to clear working directory", e);
        }
    }


}
