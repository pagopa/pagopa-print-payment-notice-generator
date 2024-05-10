package it.gov.pagopa.payment.notice.generator.model.pdf.notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Installment {

    private String refNumber;
    private String amount;
    private String expiryDate;
    private String cbillCode;
    private String qrCode;
    private String posteAuth;
    private String posteDataMatrix;
    private String posteDocumentType;

}
