package it.gov.pagopa.payment.notice.generator.model.notice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoticeRequestData {

    @Schema(description = "Notice data", requiredMode = Schema.RequiredMode.REQUIRED)
    private Notice notice;
    @Schema(description = "Creditor Institution data", requiredMode = Schema.RequiredMode.REQUIRED)
    private CreditorInstitution creditorInstitution;
    @Schema(description = "Debtor data", requiredMode = Schema.RequiredMode.REQUIRED)
    @ToString.Exclude
    private Debtor debtor;

}
