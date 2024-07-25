package it.gov.pagopa.payment.notice.generator.storage;

import com.azure.core.util.Context;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.TableServiceException;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DownloadRetryOptions;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import it.gov.pagopa.payment.notice.generator.exception.AppError;
import it.gov.pagopa.payment.notice.generator.exception.AppException;
import it.gov.pagopa.payment.notice.generator.model.TemplateResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static it.gov.pagopa.payment.notice.generator.util.WorkingDirectoryUtils.createWorkingDirectory;

@Component
@Slf4j
public class NoticeTemplateStorageClient {

    private BlobContainerClient blobContainerClient;

    private TableClient tableClient;

    private Integer maxRetry;

    private Integer timeout;

    @Autowired
    public NoticeTemplateStorageClient(
            @Value("${spring.cloud.azure.storage.blob.templates.enabled}") String enabled,
            @Value("${spring.cloud.azure.storage.blob.templates.connection_string}") String connectionString,
            @Value("${spring.cloud.azure.storage.blob.templates.containerName}") String containerName,
            @Value("${spring.cloud.azure.storage.blob.templates.tableName}") String tableName,
            @Value("${spring.cloud.azure.storage.blob.templates.retry}") Integer maxRetry,
            @Value("${spring.cloud.azure.storage.blob.templates.timeout}") Integer timeout) {
        if (Boolean.TRUE.toString().equals(enabled)) {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString).buildClient();
            tableClient = new TableClientBuilder().connectionString(connectionString)
                    .tableName(tableName).buildClient();
            blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
            this.maxRetry = maxRetry;
            this.timeout = timeout;
        }
    }

    public NoticeTemplateStorageClient(
            Boolean enabled,
            BlobContainerClient blobContainerClient,
            TableClient tableClient) {
        if (Boolean.TRUE.equals(enabled)) {
            this.blobContainerClient = blobContainerClient;
            this.tableClient = tableClient;
            this.maxRetry = 3;
            this.timeout = 10;
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
            Path workingDirectory = createWorkingDirectory().toPath().normalize().toAbsolutePath();
            File templatesDirectory = new File(workingDirectory + "/templates");
            if (!templatesDirectory.exists()) {
                Files.createDirectory(templatesDirectory.toPath());
            }
            Path filePath = workingDirectory.resolve(
                    "templates/" + templateId + ".zip").normalize().toAbsolutePath();

            if (!filePath.startsWith(workingDirectory + File.separator)) {
                throw new IllegalArgumentException("Invalid filename");
            }
            return filePath.toFile().getAbsolutePath();
        } catch (Exception e) {
            throw new AppException(AppError.TEMPLATE_CLIENT_ERROR, e);
        }
    }

    /**
     * Recovers the template list data available for notice generation from
     * Azure Table Storage
     *
     * @return template data
     */
    @Cacheable(value = "getTemplates")
    public List<TemplateResource> getTemplates() {

        if (tableClient == null) {
            throw new AppException(AppError.TEMPLATE_CLIENT_UNAVAILABLE);
        }

        try {
            return tableClient.listEntities().stream()
                    .map(item -> TemplateResource.builder()
                            .templateId(String.valueOf(item.getProperty("templateId")))
                            .description(String.valueOf(item.getProperty("description")))
                            .templateExampleUrl(String.valueOf(item.getProperty("templateExampleUrl")))
                            .templateValidationRules(String.valueOf(item.getProperty("templateValidationRules")))
                            .build())
                    .toList();
        } catch (TableServiceException tableServiceException) {
            throw new AppException(AppError.TEMPLATE_TABLE_CLIENT_ERROR, tableServiceException);
        }

    }

    @Scheduled(cron = "${spring.cloud.azure.storage.blob.templates.cron}")
    @CacheEvict(cacheNames = {"getTemplates"})
    protected void refreshTemplates() {
        File templateFiles = new File("temp/templates");
        if (templateFiles.exists()) {
            if (!templateFiles.delete()) {
                log.warn("Error while deleting template directory");
            }
        }
    }

}
