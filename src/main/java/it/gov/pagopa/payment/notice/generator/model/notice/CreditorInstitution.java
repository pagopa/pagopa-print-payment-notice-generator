package it.gov.pagopa.payment.notice.generator.model.notice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class CreditorInstitution {

    @Schema(description = "CI tax code", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @NotEmpty
    private String taxCode;

    @Schema(description = "CI full name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @NotEmpty
    private String fullName;
    private String organization;

    @Schema(description = "CI info", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @NotEmpty
    private String info;

    @Schema(description = "Boolean to refer if it has a web channel", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Boolean webChannel;

    @Schema(description = "CI physical channel data")
    private String physicalChannel;

    @Schema(description = "CI cbill", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @NotEmpty
    private String cbill;


}
