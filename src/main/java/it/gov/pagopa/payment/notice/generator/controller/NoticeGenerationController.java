package it.gov.pagopa.payment.notice.generator.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.payment.notice.generator.exception.AppError;
import it.gov.pagopa.payment.notice.generator.exception.AppException;
import it.gov.pagopa.payment.notice.generator.model.NoticeGenerationRequestItem;
import it.gov.pagopa.payment.notice.generator.service.NoticeGenerationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
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

import static it.gov.pagopa.payment.notice.generator.util.WorkingDirectoryUtils.clearTempDirectory;

/**
 * Controller containing APIs to generate notice
 */
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

    /**
     * POST method to generate a single notice, if a folderId is provided the content will be saved inside the provided
     * folder
     * @param folderId optional parameter to use if the content generates has to be saved
     * @param noticeGenerationRequestItem data containing notice generation request
     * @return generated pdf
     */
    @PostMapping("/generate")
    public ResponseEntity<Resource> generateNotice(
            @RequestParam(value = "folderId", required = false) String folderId,
            @Parameter(description = "templateId to use for retrieval")
            @Valid @NotNull @RequestBody NoticeGenerationRequestItem noticeGenerationRequestItem) {
        File file = noticeGenerationService.generateNotice(noticeGenerationRequestItem, folderId);
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .headers(headers)
                    .body(new ByteArrayResource(inputStream.readAllBytes()));
        } catch (Exception e) {
            throw new AppException(AppError.INTERNAL_SERVER_ERROR, e);
        } finally {
            clearTempDirectory(file.toPath().getParent());
        }
    }

}
