package it.gov.pagopa.payment.notice.generator.model.pdf.notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payee {

    private String logo;
    private String name;
    private String taxCode;
    private String sector;
    private String additionalInfo;
    private Channel channel;

}
