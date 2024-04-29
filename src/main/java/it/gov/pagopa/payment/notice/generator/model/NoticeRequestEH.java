package it.gov.pagopa.payment.notice.generator.model;

import lombok.Data;

@Data
public class NoticeRequestEH {

    private String folderId;
    private NoticeGenerationRequestItem noticeGenerationRequestItem;

}
