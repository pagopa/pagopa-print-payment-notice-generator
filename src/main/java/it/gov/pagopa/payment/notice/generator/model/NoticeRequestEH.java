package it.gov.pagopa.payment.notice.generator.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoticeRequestEH {

    @NotNull
    private String folderId;
    private NoticeGenerationRequestItem noticeData;

}
