
package it.gov.pagopa.payment.notice.generator.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResource {

    @Schema(description = "templateId, to use when requiring generation of a notice with the selected template", requiredMode = Schema.RequiredMode.REQUIRED)
    private String templateId;
    @Schema(description = "Template description", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;
    @Schema(description = "Template example url", requiredMode = Schema.RequiredMode.REQUIRED)
    private String templateExampleUrl;
    @Schema(description = "Template validation rules in JSON format")
    private String templateValidationRules;

}
