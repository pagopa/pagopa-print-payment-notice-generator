package it.gov.pagopa.payment.notice.generator.storage;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import it.gov.pagopa.payment.notice.generator.exception.AppException;
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
class NoticeTemplateStorageClientTest {

    private BlobContainerClient blobContainerClient;

    private static BlobClient blobClientMock;

    private NoticeTemplateStorageClient noticeTemplateStorageClient;

    @BeforeEach
    public void init() {
        blobContainerClient = mock(BlobContainerClient.class);

        noticeTemplateStorageClient = new NoticeTemplateStorageClient(
                true, blobContainerClient);
        blobClientMock = mock(BlobClient.class);
        lenient().doReturn(blobClientMock).when(blobContainerClient).getBlobClient(anyString());
    }

    @Test
    void shouldReturnTemplate() {
        doReturn(null).when(blobClientMock)
                .downloadToFileWithResponse(
                        any(BlobDownloadToFileOptions.class),
                        any(Duration.class),
                        any(Context.class)
                );
        File result = noticeTemplateStorageClient.getTemplate("testFile");
        assertNotNull(result);
        noticeTemplateStorageClient.refreshTemplates();
    }

    @Test
    void shouldReturnException() {
        doThrow(new BlobStorageException("test", null, null)).when(blobClientMock)
                .downloadToFileWithResponse(
                        any(BlobDownloadToFileOptions.class),
                        any(Duration.class),
                        any(Context.class)
                );
        assertThrows(AppException.class, () -> noticeTemplateStorageClient.getTemplate("testFile"));
    }

    @Test
    void shouldReturnExceptionOnMissingClient() {
        assertThrows(AppException.class, () ->
                new NoticeTemplateStorageClient(false, null)
                        .getTemplate("testFile"));
    }

    @Test
    void shouldReturnKOOnWrongFile() {
        assertThrows(AppException.class, () ->
                noticeTemplateStorageClient.getTemplate("../../testFile"));
    }

}
