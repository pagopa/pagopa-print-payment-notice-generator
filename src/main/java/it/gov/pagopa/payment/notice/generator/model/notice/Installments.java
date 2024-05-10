package it.gov.pagopa.payment.notice.generator.model.notice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class Installments {

    @Schema(description = "Number of installments")
    private Integer number;
    @Schema(description = "Installments of the notice")
    private List<InstallmentData> data;

}
