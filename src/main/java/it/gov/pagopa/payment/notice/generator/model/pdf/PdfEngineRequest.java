package it.gov.pagopa.payment.notice.generator.model.pdf;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URL;

/**
 * Model class for PDF engine request
 */
@Getter
@Setter
@NoArgsConstructor
public class PdfEngineRequest {

    URL template;
    String data;
    boolean applySignature;
}
