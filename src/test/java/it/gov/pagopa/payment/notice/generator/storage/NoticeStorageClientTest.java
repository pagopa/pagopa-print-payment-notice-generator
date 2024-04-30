
package it.gov.pagopa.payment.notice.generator.storage;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeStorageClientTest {

    @Test
    void runOk() {
        BlobContainerClient mockContainer = mock(BlobContainerClient.class);
        BlobClient mockClient = mock(BlobClient.class);

        Response mockBlockItem = mock(Response.class);

        when(mockBlockItem.getStatusCode()).thenReturn(HttpStatus.CREATED.value());

        when(mockClient.uploadWithResponse(any(), eq(null), eq(null))).thenReturn(
                mockBlockItem
        );

        when(mockContainer.getBlobClient(any())).thenReturn(mockClient);

        NoticeStorageClient noticeStorageClient = new NoticeStorageClient(true, mockContainer);

        boolean result = noticeStorageClient.savePdfToBlobStorage(
                InputStream.nullInputStream(), "filename");

        assertTrue(result);

    }

    @Test
    void runKo() {
        BlobContainerClient mockContainer = mock(BlobContainerClient.class);
        BlobClient mockClient = mock(BlobClient.class);

        Response mockBlockItem = mock(Response.class);

        when(mockBlockItem.getStatusCode()).thenReturn(HttpStatus.NO_CONTENT.value());

        when(mockClient.uploadWithResponse(any(), eq(null), eq(null))).thenReturn(
                mockBlockItem
        );

        when(mockContainer.getBlobClient(any())).thenReturn(mockClient);

        NoticeStorageClient receiptBlobClient = new NoticeStorageClient(true, mockContainer);

        boolean response = receiptBlobClient.savePdfToBlobStorage(InputStream.nullInputStream(), "filename");

        Assertions.assertFalse(response);

    }

}
