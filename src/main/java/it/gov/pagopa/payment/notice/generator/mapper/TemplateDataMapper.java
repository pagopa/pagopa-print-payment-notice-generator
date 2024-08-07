package it.gov.pagopa.payment.notice.generator.mapper;

import it.gov.pagopa.payment.notice.generator.model.notice.InstallmentData;
import it.gov.pagopa.payment.notice.generator.model.notice.NoticeRequestData;
import it.gov.pagopa.payment.notice.generator.model.pdf.notice.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

/**
 * Class containing methods to map paymentNotice data to use in notice generation
 */
public class TemplateDataMapper {

    private static final String POSTE_DOCUMENT_TYPE_CODE = "896";

    /**
     * Map notice generation dat
     *
     * @param noticeRequestData request data
     * @return mapped notice data
     */
    public static PaymentNotice mapTemplate(NoticeRequestData noticeRequestData) {

        String noticeCode = noticeRequestData.getNotice().getCode();
        String cbill = noticeRequestData.getCreditorInstitution().getCbill();
        String ciTaxCode = noticeRequestData.getCreditorInstitution().getTaxCode();
        String noticeAmount = noticeRequestData.getNotice().getPaymentAmount() != null ?
                String.valueOf(noticeRequestData.getNotice().getPaymentAmount()) : null;
        InstallmentData reduced = noticeRequestData.getNotice().getReduced();
        InstallmentData discounted = noticeRequestData.getNotice().getDiscounted();
        String debtorTaxCode = noticeRequestData.getDebtor().getTaxCode();
        String fullName = noticeRequestData.getDebtor().getFullName();
        String subject = noticeRequestData.getNotice().getSubject();
        String posteAuthCode = noticeRequestData.getCreditorInstitution().getPosteAuth();
        String posteAccountNumber = noticeRequestData.getCreditorInstitution().getPosteAccountNumber();


        return PaymentNotice.builder()
                .payee(Payee.builder()
                        .taxCode(ciTaxCode)
                        .name(noticeRequestData.getCreditorInstitution().getFullName())
                        .channel(Channel.builder().online(
                                        Online.builder()
                                                .website(Objects.requireNonNullElse(noticeRequestData.getCreditorInstitution().getWebChannel(), false))
                                                .app(Objects.requireNonNullElse(noticeRequestData.getCreditorInstitution().getAppChannel(), false))
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
                        .qrCode(noticeAmount != null ?
                                generateQrCode(noticeCode,
                                        ciTaxCode,
                                        noticeAmount) : null
                        )
                        .subject(noticeRequestData.getNotice().getSubject())
                        .amount(noticeAmount != null ? currencyFormat(noticeAmount) : null)
                        .posteAuth(posteAuthCode)
                        .posteAccountNumber(posteAccountNumber)
                        .posteDocumentType(POSTE_DOCUMENT_TYPE_CODE)
                        .expiryDate(noticeRequestData.getNotice().getDueDate())
                        .posteDataMatrix(posteAuthCode != null ?
                                generatePosteDataMatrix(
                                        ciTaxCode,
                                        debtorTaxCode,
                                        fullName,
                                        subject,
                                        posteAccountNumber,
                                        noticeAmount,
                                        POSTE_DOCUMENT_TYPE_CODE,
                                        noticeCode
                                ) : null)
                        .instalments(Installments.builder()
                                .reduced(
                                        reduced != null ? mapInstallment(
                                                cbill,
                                                ciTaxCode,
                                                debtorTaxCode,
                                                fullName,
                                                subject,
                                                posteAccountNumber,
                                                posteAuthCode,
                                                reduced
                                        ) : null
                                )
                                .discounted(
                                        discounted != null ? mapInstallment(
                                                cbill,
                                                ciTaxCode,
                                                debtorTaxCode,
                                                fullName,
                                                subject,
                                                posteAccountNumber,
                                                posteAuthCode,
                                                discounted
                                        ) : null
                                )
                                .items(noticeRequestData.getNotice().getInstallments() != null ?
                                        noticeRequestData.getNotice().getInstallments().stream().map(item ->
                                                mapInstallment(
                                                        cbill,
                                                        ciTaxCode,
                                                        debtorTaxCode,
                                                        fullName,
                                                        subject,
                                                        posteAccountNumber,
                                                        posteAuthCode,
                                                        item
                                                )).toList() :
                                        Collections.emptyList()).build())
                        .build())
                .build();
    }

    private static String generatePosteDataMatrix(
            String ciTaxCode, String debtorTaxCode, String fullName, String subject,
            String posteAccountNumber, String amount, String posteTypeCode, String noticeCode
    ) {
        return String.join("",
                "codfase=NBPA;",
                generateCodeline(noticeCode, posteAccountNumber, amount, posteTypeCode),
                "1P1",
                StringUtils.rightPad(ciTaxCode, 11, " "),
                StringUtils.rightPad(debtorTaxCode, 16, " "),
                StringUtils.rightPad(fullName, 40, " "),
                StringUtils.rightPad(subject, 110, " "),
                StringUtils.rightPad("", 12, " "),
                "A");
    }

    private static String generateCodeline(String noticeCode, String posteAccountNumber,
                                           String amount, String posteTypeCode) {
        return String.join("", "18",
                StringUtils.leftPad(noticeCode, 18, "0"),
                "12",
                StringUtils.leftPad(posteAccountNumber, 12, "0"),
                "10",
                StringUtils.leftPad(amount, 10, "0"), "3",
                posteTypeCode);
    }

    private static String generateQrCode(String code, String taxCode, String amount) {
        return String.join("|",
                "PAGOPA", "002",
                StringUtils.leftPad(code, 18, "0"),
                StringUtils.leftPad(taxCode, 11, "0"),
                amount
        );
    }

    private static Installment mapInstallment(
            String cbill, String ciTaxCode, String debtorTaxCode,
            String fullname, String subject, String accountNumber,
            String posteAuth, InstallmentData installmentData) {
        String amount = String.valueOf(installmentData.getAmount());
        return Installment.builder()
                .refNumber(installmentData.getCode())
                .cbillCode(cbill)
                .qrCode(generateQrCode(installmentData.getCode(), ciTaxCode, amount))
                .amount(currencyFormat(amount))
                .expiryDate(installmentData.getDueDate())
                .posteDocumentType(POSTE_DOCUMENT_TYPE_CODE)
                .posteAuth(posteAuth)
                .posteDataMatrix(posteAuth != null ?
                        generatePosteDataMatrix(
                                ciTaxCode,
                                debtorTaxCode,
                                fullname,
                                subject,
                                accountNumber,
                                String.valueOf(installmentData.getAmount()),
                                POSTE_DOCUMENT_TYPE_CODE,
                                installmentData.getCode()
                        ) :
                        null
                )
                .build();
    }

    private static String currencyFormat(String value) {
        BigDecimal amount = new BigDecimal(value);
        BigDecimal divider = new BigDecimal(100);
        BigDecimal valueToFormat = amount.divide(divider, 2, RoundingMode.UNNECESSARY);
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ITALY);
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);
        return numberFormat.format(valueToFormat);
    }

}
