package it.gov.pagopa.payment.notice.generator.mapper;

import it.gov.pagopa.payment.notice.generator.model.notice.InstallmentData;
import it.gov.pagopa.payment.notice.generator.model.notice.NoticeRequestData;
import it.gov.pagopa.payment.notice.generator.model.pdf.notice.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;

public class TemplateDataMapper {

    public static PaymentNotice mapTemplate(NoticeRequestData noticeRequestData) {

        String noticeCode = noticeRequestData.getNotice().getCode();
        String cbill = noticeRequestData.getCreditorInstitution().getCbill();
        String ciTaxCode = noticeRequestData.getCreditorInstitution().getTaxCode();
        String noticeAmount = String.valueOf(noticeRequestData.getNotice().getPaymentAmount());

        return PaymentNotice.builder()
                .payee(Payee.builder()
                        .taxCode(ciTaxCode)
                        .name(noticeRequestData.getDebtor().getFullName())
                        .channel(Channel.builder().online(
                                Online.builder()
                                        .website(noticeRequestData.getCreditorInstitution().getWebChannel())
                                        .app(!noticeRequestData.getCreditorInstitution().getWebChannel())
                                        .build())
                                .physical(Physical.builder()
                                        .data(noticeRequestData.getCreditorInstitution().getPhysicalChannel())
                                        .build())
                                .build()
                        )
                        .logo(noticeRequestData.getCreditorInstitution().getLogo())
                        .additionalInfo(noticeRequestData.getCreditorInstitution().getInfo())
                        .sector(noticeRequestData.getCreditorInstitution().getOrganization())
                        .build()
                )
                .debtor(Debtor
                        .builder()
                        .buildingNumber(noticeRequestData.getDebtor().getBuildingNumber())
                        .province(noticeRequestData.getDebtor().getProvince())
                        .city(noticeRequestData.getDebtor().getCity())
                        .postalCode(noticeRequestData.getDebtor().getPostalCode())
                        .address(noticeRequestData.getDebtor().getAddress())
                        .fullName(noticeRequestData.getDebtor().getFullName())
                        .taxCode(noticeRequestData.getDebtor().getTaxCode())
                        .build()
                )
                .notice(Notice.builder()
                        .refNumber(noticeCode)
                        .cbillCode(cbill)
                        .qrCode(
                                generateQrCode(noticeCode,
                                ciTaxCode,
                                noticeAmount)
                        )
                        .subject(noticeRequestData.getNotice().getSubject())
                        .amount(noticeAmount)
                        .expiryDate(noticeRequestData.getNotice().getDueDate())
                        .instalments(noticeRequestData.getNotice().getInstallments() != null ?
                                noticeRequestData.getNotice().getInstallments().stream().map(item ->
                                        mapInstallment(
                                                noticeCode,
                                                cbill,
                                                item
                                        )).toList() :
                                Collections.emptyList())
                        .build())
                .build();
    }

    private static String generateQrCode(String code, String taxCode, String amount) {
        return String.join("|",
                "PAGOPA","002",
                StringUtils.leftPad(code, 18, "0"),
                StringUtils.leftPad(code, 11, "0"),
                taxCode,
                amount
        );
    }

    private static Installment mapInstallment(
            String cbill, String taxCode, InstallmentData installmentData) {
        String amount = String.valueOf(installmentData.getAmount());
        return Installment.builder()
                .refNumber(installmentData.getCode())
                .cbillCode(cbill)
                .qrCode(generateQrCode(installmentData.getCode(), taxCode, amount))
                .amount(amount)
                .expiryDate(installmentData.getDueDate())
                .build();
    }

}
