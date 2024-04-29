package it.gov.pagopa.payment.notice.generator.client;

import it.gov.pagopa.payment.notice.generator.model.pdf.PdfEngineRequest;
import it.gov.pagopa.payment.notice.generator.model.pdf.PdfEngineResponse;

import java.nio.file.Path;

public interface PdfEngineClient {

    PdfEngineResponse generatePDF(PdfEngineRequest pdfEngineRequest, Path workingDirPath);

}
