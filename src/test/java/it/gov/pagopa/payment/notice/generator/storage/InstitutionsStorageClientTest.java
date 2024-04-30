package it.gov.pagopa.payment.notice.generator.storage;

import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.payment.notice.generator.exception.AppException;
import it.gov.pagopa.payment.notice.generator.model.notice.CreditorInstitution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstitutionsStorageClientTest {

    private BlobContainerClient blobContainerClient;

    private static BlobClient blobClientMock;

    private ObjectMapper objectMapper = new ObjectMapper();

    private InstitutionsStorageClient institutionsStorageClient;

    @BeforeEach
    public void init() {
        blobContainerClient = mock(BlobContainerClient.class);
        institutionsStorageClient = new InstitutionsStorageClient(
                true, blobContainerClient, objectMapper);
        blobClientMock = mock(BlobClient.class);
        lenient().doReturn(blobClientMock).when(blobContainerClient).getBlobClient(anyString());
    }

    @Test
    void shouldReturnCreditorInstitutions() throws JsonProcessingException {
        doReturn(BinaryData.fromBytes(objectMapper.writeValueAsString(
                CreditorInstitution.builder().build()).getBytes()))
                .when(blobClientMock)
                .downloadContent();
        CreditorInstitution result = institutionsStorageClient.getInstitutionData("testFile");
        assertNotNull(result);
    }

    @Test
    void shouldReturnException() {
        doThrow(new BlobStorageException("test", null, null)).when(blobClientMock)
                .downloadContent();
        assertThrows(AppException.class, () -> institutionsStorageClient.getInstitutionData("testFile"));
    }

    @Test
    void shouldReturnExceptionOnMissingClient() {
        assertThrows(AppException.class, () ->
                new InstitutionsStorageClient(false, null, null)
                        .getInstitutionData("testFile"));
    }

}
