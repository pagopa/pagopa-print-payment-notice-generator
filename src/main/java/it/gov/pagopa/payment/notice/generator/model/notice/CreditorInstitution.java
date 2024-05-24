package it.gov.pagopa.payment.notice.generator.model.notice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreditorInstitution {

    @Schema(description = "CI tax code", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @NotEmpty
    private String taxCode;

    @Schema(description = "CI full name")
    private String fullName;

    @Schema(description = "CI Organization")
    private String organization;

    @Schema(description = "CI info")
    private String info;

    @Schema(description = "Boolean to refer if it has a web channel")
    @NotNull
    private Boolean webChannel;

    @Schema(description = "CI physical channel data")
    private String physicalChannel;

    @Schema(description = "CI cbill")
    private String cbill;

    private String logo;

    @Schema(description = "Installment poste auth code")
    private String posteAuth;

    @Schema(description = "Poste account number")
    private String posteAccountNumber;



}
