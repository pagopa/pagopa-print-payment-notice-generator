package it.gov.pagopa.payment.notice.generator.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AppError {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Something was wrong"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad Request", "%s"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized", "Error during authentication"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden", "This method is forbidden"),
    RESPONSE_NOT_READABLE(HttpStatus.BAD_GATEWAY, "Response Not Readable", "The response body is not readable"),

    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "Template Not Found",
            "Required template has not been found on the storage"),
    TEMPLATE_CLIENT_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
            "Template Storage Not Available",
            "Template Storage client temporarily not available"),
    TEMPLATE_CLIENT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Template Client Error",
            "Template Client encountered an error"),

    FOLDER_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "Folder Not Available",
            "Required folder is either missing or not available to require"),

    INSTITUTION_NOT_FOUND(HttpStatus.PRECONDITION_FAILED, "Institution Not Found",
            "Required institution data has not been found on the storage"),

    INSTITUTION_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Parsing Error for Institution Data",
            "Exception thrown while parsing institution data retrieve from storage"),

    MESSAGE_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Message Validation Error" , "EH Message content is not valid, with errors: %s"),

    PDF_ENGINE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PDF Engine Error", "Encountered an error calling the PDF Engine"),

    NOTICE_SAVE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Notice save error", "Exception while saving notice"),

    TEMPLATE_TABLE_CLIENT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Template Table Client Error",
            "Template Table Client encountered an error"),

    UNKNOWN(null, null, null);

  public final HttpStatus httpStatus;
  public final String title;
  public final String details;


  AppError(HttpStatus httpStatus, String title, String details) {
    this.httpStatus = httpStatus;
    this.title = title;
    this.details = details;
  }
}


