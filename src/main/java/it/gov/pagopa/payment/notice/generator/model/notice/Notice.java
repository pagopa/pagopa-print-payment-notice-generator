package it.gov.pagopa.payment.notice.generator.model.notice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notice {

    @Schema(description = "Notice subject", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @NotEmpty
    private String subject;

    @Schema(description = "Notice total amount to pay", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long paymentAmount;

    @Schema(description = "Notice due date", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dueDate;

    @Schema(description = "Notice code", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @NotEmpty
    private String code;

    @Schema(description = "Poste Document Type related to main notice")
    private String posteDocumentType;

    @Schema(description = "Notice installments (if present)")
    private List<InstallmentData> installments;

}
