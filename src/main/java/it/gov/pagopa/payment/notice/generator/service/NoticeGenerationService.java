package it.gov.pagopa.payment.notice.generator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.payment.notice.generator.model.NoticeGenerationRequestItem;

import java.io.File;
import java.net.MalformedURLException;

public interface NoticeGenerationService {
    File generateNotice(NoticeGenerationRequestItem noticeGenerationRequestItem,
                        String folderId) ;
}
