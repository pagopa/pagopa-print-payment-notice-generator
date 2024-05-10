package it.gov.pagopa.payment.notice.generator.model;

import it.gov.pagopa.payment.notice.generator.model.notice.NoticeRequestData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoticeGenerationRequestItem {

    private String templateId;
    private NoticeRequestData data;

}
