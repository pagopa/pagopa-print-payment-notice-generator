package it.gov.pagopa.payment.notice.generator.storage;

import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DownloadRetryOptions;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.notice.generator.exception.AppError;
import it.gov.pagopa.payment.notice.generator.exception.AppException;
import it.gov.pagopa.payment.notice.generator.model.notice.CreditorInstitution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

@Component
@Slf4j
public class InstitutionsStorageClient {

    private BlobContainerClient blobContainerClient;

    private ObjectMapper objectMapper;

    @Autowired
    public InstitutionsStorageClient(
            @Value("${spring.cloud.azure.storage.blob.institutions.enabled}") String enabled,
            @Value("${spring.cloud.azure.storage.blob.institutions.connection_string}") String connectionString,
            @Value("${spring.cloud.azure.storage.blob.institutions.containerName}") String containerName,
            ObjectMapper objectMapper) {
        if (Boolean.TRUE.toString().equals(enabled)) {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString).buildClient();
            blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
            this.objectMapper = objectMapper;
        }
    }
    public InstitutionsStorageClient(
            Boolean enabled,
            BlobContainerClient blobContainerClient,
            ObjectMapper objectMapper) {
        if (Boolean.TRUE.equals(enabled)) {
             this.blobContainerClient = blobContainerClient;
             this.objectMapper = objectMapper;
        }
    }

    /**
     * Retrieve the institutionData from the Blob Storage
     *
     * @param institutionCode the name of the institution to be retrieved
     * @return the File with the reference to the downloaded data
     * @throws AppException thrown for error when retrieving the data
     */
    public CreditorInstitution getInstitutionData(String institutionCode) {
        if (blobContainerClient == null) {
            throw new AppException(AppError.TEMPLATE_CLIENT_UNAVAILABLE);
        }
        try {
            BinaryData jsonData = blobContainerClient.getBlobClient(institutionCode.concat("/data.json"))
                    .downloadContent();
            return objectMapper.readValue(jsonData.toBytes(), CreditorInstitution.class);
        } catch (BlobStorageException blobStorageException) {
            log.error(blobStorageException.getMessage(), blobStorageException);
            throw new AppException(AppError.INSTITUTION_NOT_FOUND, blobStorageException);
        } catch (IOException ioException) {
            log.error(ioException.getMessage(), ioException);
            throw new AppException(AppError.INSTITUTION_PARSING_ERROR, ioException);

        }
    }

}
