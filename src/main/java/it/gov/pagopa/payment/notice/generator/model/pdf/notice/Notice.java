package it.gov.pagopa.payment.notice.generator.model.pdf.notice;

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

    private String subject;
    private String amount;
    private String expiryDate;
    private String qrCode;
    private String refNumber;
    private String cbillCode;
    private List<Installment> instalments;

}
