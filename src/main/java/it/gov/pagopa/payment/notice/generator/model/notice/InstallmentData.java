package it.gov.pagopa.payment.notice.generator.model.notice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class InstallmentData {

    @Schema(description = "Installment amount", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer amount;
    @Schema(description = "Installment dueDate", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dueDate;

}
