package it.gov.pagopa.payment.notice.generator.service;

import it.gov.pagopa.payment.notice.generator.model.NoticeGenerationRequestItem;

import java.io.File;

public interface NoticeGenerationService {
    File generateNotice(NoticeGenerationRequestItem noticeGenerationRequestItem,
                        String folderId) ;

    void processNoticeGenerationEH(String message);
}
