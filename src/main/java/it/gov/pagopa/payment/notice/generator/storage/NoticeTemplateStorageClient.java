package it.gov.pagopa.payment.notice.generator.storage;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DownloadRetryOptions;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import it.gov.pagopa.payment.notice.generator.exception.AppError;
import it.gov.pagopa.payment.notice.generator.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

import static it.gov.pagopa.payment.notice.generator.util.WorkingDirectoryUtils.createWorkingDirectory;

@Component
@Slf4j
public class NoticeTemplateStorageClient {

    private BlobContainerClient blobContainerClient;

    private Integer maxRetry;

    private Integer timeout;

    @Autowired
    public NoticeTemplateStorageClient(
            @Value("${spring.cloud.azure.storage.blob.templates.enabled}") String enabled,
            @Value("${spring.cloud.azure.storage.blob.templates.connection_string}") String connectionString,
            @Value("${spring.cloud.azure.storage.blob.templates.containerName}") String containerName,
            @Value("${spring.cloud.azure.storage.blob.templates.retry}") Integer maxRetry,
            @Value("${spring.cloud.azure.storage.blob.templates.timeout}") Integer timeout) {
        if (Boolean.TRUE.toString().equals(enabled)) {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString).buildClient();
            blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
            this.maxRetry = maxRetry;
            this.timeout = timeout;
        }
    }
    public NoticeTemplateStorageClient(
            Boolean enabled,
            BlobContainerClient blobContainerClient) {
        if (Boolean.TRUE.equals(enabled)) {
             this.blobContainerClient = blobContainerClient;
             this.maxRetry=3;
             this.timeout=10;
        }
    }

    /**
     * Retrieve the template from the Blob Storage
     *
     * @param templateId the name of the file to be retrieved
     * @return the File with the reference to the downloaded template
     * @throws AppException thrown for error when retrieving the template
     */
    public File getTemplate(String templateId) {

        if (blobContainerClient == null) {
            throw new AppException(AppError.TEMPLATE_CLIENT_UNAVAILABLE);
        }
        String filePath = createTemplatesDirectory(templateId);
        try {
            if (!new File(filePath).exists()) {
                blobContainerClient.getBlobClient(templateId.concat("/template.zip"))
                        .downloadToFileWithResponse(
                                getBlobDownloadToFileOptions(filePath),
                                Duration.ofSeconds(timeout),
                                Context.NONE);
            }
            return new File(filePath);
        } catch (BlobStorageException blobStorageException) {
            throw new AppException(AppError.TEMPLATE_NOT_FOUND, blobStorageException);
        }
    }

    private BlobDownloadToFileOptions getBlobDownloadToFileOptions(String filePath) {
        return new BlobDownloadToFileOptions(filePath)
                .setDownloadRetryOptions(new DownloadRetryOptions().setMaxRetryRequests(maxRetry))
                .setOpenOptions(new HashSet<>(
                        Arrays.asList(
                                StandardOpenOption.CREATE_NEW,
                                StandardOpenOption.WRITE,
                                StandardOpenOption.READ
                        ))
                );
    }

    private String createTemplatesDirectory(String templateId) {
        try {
            File workingDirectory = createWorkingDirectory();
            Path filePath = workingDirectory.toPath().resolve(
                    "templates/"+templateId + ".zip").normalize().toAbsolutePath();
            if (!filePath.startsWith(workingDirectory + File.separator)) {
                throw new IllegalArgumentException("Invalid filename");
            }
            return filePath.toFile().getAbsolutePath();
        } catch (IOException e) {
            throw new AppException(AppError.TEMPLATE_CLIENT_ERROR, e);
        }
    }

    @Scheduled(cron = "${spring.cloud.azure.storage.blob.templates.timeout}")
    private void refreshTemplates() {
        File templateFiles = new File("temp/templates");
        if (templateFiles.exists()) {
            if (!templateFiles.delete()) {
                log.warn("Error while deleting template directory");
            }
        }
    }

}
