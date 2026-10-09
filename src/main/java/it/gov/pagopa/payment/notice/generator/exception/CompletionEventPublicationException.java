package it.gov.pagopa.payment.notice.generator.exception;

/**
 * Raised when a folder has completed notice generation but the completion
 * event required to start the compression phase cannot be published.
 */
public class CompletionEventPublicationException extends RuntimeException {

	private static final long serialVersionUID = -647526083946125834L;

	public CompletionEventPublicationException(String folderId) {
        super("Unable to publish completion event for folder " + folderId);
    }

    public CompletionEventPublicationException(
            String folderId,
            Throwable cause) {

        super(
                "Unable to publish completion event for folder " + folderId,
                cause);
    }
}