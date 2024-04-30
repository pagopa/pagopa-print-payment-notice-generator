package it.gov.pagopa.payment.notice.generator.storage;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class NoticeStorageClient {

    private BlobContainerClient blobContainerClient;

    @Autowired
    public NoticeStorageClient(
            @Value("${spring.cloud.azure.storage.blob.notices.enabled}") String enabled,
            @Value("${spring.cloud.azure.storage.blob.notices.connection_string}") String connectionString,
            @Value("${spring.cloud.azure.storage.blob.notices.containerName}") String containerName) {
        if (Boolean.TRUE.toString().equals(enabled)) {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString).buildClient();
            blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        }
    }
    public NoticeStorageClient(
            Boolean enabled,
            BlobContainerClient blobContainerClient) {
        if (Boolean.TRUE.equals(enabled)) {
             this.blobContainerClient = blobContainerClient;
        }
    }

    /**
     * Handles saving the PDF to the blob storage
     *
     * @param pdf      PDF file
     * @param fileName Filename to save the PDF with
     * @return blob storage response with PDF metadata or error message and status
     */
    public boolean savePdfToBlobStorage(InputStream pdf, String fileName) {

        //Get a reference to a blob
        BlobClient blobClient = blobContainerClient.getBlobClient(fileName);

        //Upload the blob
        Response<BlockBlobItem> blockBlobItemResponse = blobClient.uploadWithResponse(
                new BlobParallelUploadOptions(
                        pdf
                ), null, null);

        //Build response accordingly
        int statusCode = blockBlobItemResponse.getStatusCode();

        return statusCode == HttpStatus.CREATED.value();

    }

}
