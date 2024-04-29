package it.gov.pagopa.payment.notice.generator.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AppError {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Something was wrong"),
    BAD_REQUEST(HttpStatus.INTERNAL_SERVER_ERROR, "Bad Request", "%s"),
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

    INSTITUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Institution Not Found",
            "Required institution data has not been found on the storage"),

    INSTITUTION_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Parsing Error for Institution Data",
            "Exception thrown while parsing institution data retrieve from storage"),
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


