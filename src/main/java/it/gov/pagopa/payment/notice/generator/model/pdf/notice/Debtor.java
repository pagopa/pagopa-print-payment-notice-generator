package it.gov.pagopa.payment.notice.generator.model.pdf.notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Debtor {

    private String fullName;
    private String taxCode;
    private String address;
    private String buildingNumber;
    private String postalCode;
    private String city;
    private String province;

}
