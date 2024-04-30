package it.gov.pagopa.payment.notice.generator.mapper;

import it.gov.pagopa.payment.notice.generator.model.notice.InstallmentData;
import it.gov.pagopa.payment.notice.generator.model.notice.NoticeRequestData;
import it.gov.pagopa.payment.notice.generator.model.pdf.notice.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;

/**
 * Class containing methods to map paymentNotice data to use in notice generation
 */
public class TemplateDataMapper {

    /**
     * Map notice generation dat
     * @param noticeRequestData request data
     * @return mapped notice data
     */
    public static PaymentNotice mapTemplate(NoticeRequestData noticeRequestData) {

        String noticeCode = noticeRequestData.getNotice().getCode();
        String cbill = noticeRequestData.getCreditorInstitution().getCbill();
        String ciTaxCode = noticeRequestData.getCreditorInstitution().getTaxCode();
        String noticeAmount = String.valueOf(noticeRequestData.getNotice().getPaymentAmount());
        String debtorTaxCode = noticeRequestData.getDebtor().getTaxCode();
        String fullName = noticeRequestData.getDebtor().getFullName();
        String subject = noticeRequestData.getNotice().getSubject();
        String posteAuthCode = noticeRequestData.getNotice().getPosteAuth();
        String posteAccountNumber = noticeRequestData.getCreditorInstitution().getPosteAccountNumber();


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
                        .fullName(fullName)
                        .taxCode(debtorTaxCode)
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
                        .posteDataMatrix(posteAuthCode != null ?
                                generatePosteDataMatrix(
                                ciTaxCode,
                                debtorTaxCode,
                                fullName,
                                subject,
                                posteAuthCode,
                                posteAccountNumber,
                                noticeAmount,
                                noticeRequestData.getNotice().getPosteDocumentType()
                        ) : null)
                        .instalments(noticeRequestData.getNotice().getInstallments() != null ?
                                noticeRequestData.getNotice().getInstallments().stream().map(item ->
                                        mapInstallment(
                                                ciTaxCode,
                                                cbill,
                                                debtorTaxCode,
                                                fullName,
                                                subject,
                                                posteAccountNumber,
                                                item
                                        )).toList() :
                                Collections.emptyList())
                        .build())
                .build();
    }

    private static String generatePosteDataMatrix(
            String ciTaxCode, String debtorTaxCode, String fullName, String subject, String authCode,
            String posteAccountNumber, String amount, String posteTypeCode
    ) {
        return String.join("",
                "codfase=NBPA;",
                generateCodeline(authCode, posteAccountNumber, amount, posteTypeCode),
                "1P1",
                StringUtils.rightPad(ciTaxCode, 11, " "),
                StringUtils.rightPad(debtorTaxCode, 16, " "),
                StringUtils.rightPad(fullName, 40, " "),
                StringUtils.rightPad(subject, 110, " "),
                StringUtils.rightPad("", 12, " "),
                "A");
    }

    private static String generateCodeline(String posteAuthCode, String posteAccountNumber,
                                           String amount, String posteTypeCode) {
        return String.join("","18",
                StringUtils.leftPad(posteAuthCode, 18, "0"),
                "12",
                StringUtils.leftPad(posteAccountNumber, 12, "0"),
                "10",
                StringUtils.leftPad(amount, 10, "0"), "3",
                posteTypeCode);
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
            String cbill, String ciTaxCode, String debtorTaxCode,
            String fullname, String subject, String accountNumber,
            InstallmentData installmentData) {
        String amount = String.valueOf(installmentData.getAmount());
        return Installment.builder()
                .refNumber(installmentData.getCode())
                .cbillCode(cbill)
                .qrCode(generateQrCode(installmentData.getCode(), ciTaxCode, amount))
                .amount(amount)
                .expiryDate(installmentData.getDueDate())
                .posteDocumentType(installmentData.getPosteDocumentType())
                .posteAuth(installmentData.getPosteAuth())
                .posteDataMatrix(installmentData.getPosteAuth() != null ?
                    generatePosteDataMatrix(
                            ciTaxCode,
                            debtorTaxCode,
                            fullname,
                            subject,
                            installmentData.getPosteAuth(),
                            accountNumber,
                            String.valueOf(installmentData.getAmount()),
                            installmentData.getPosteDocumentType()
                    ) :
                        null
                )
                .build();
    }

}
