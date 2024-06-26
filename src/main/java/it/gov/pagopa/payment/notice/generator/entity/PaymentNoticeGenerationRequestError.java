package it.gov.pagopa.payment.notice.generator.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "errorId")
@Document("payment_notice_generation_request_error")
@ToString
public class PaymentNoticeGenerationRequestError {

    @Id
    private String id;

    @Indexed()
    private String folderId;

    @Indexed()
    private String errorId;

    @CreatedDate
    private Instant createdAt;

    private String errorCode;

    private String errorDescription;

    private String data;

    private Integer numberOfAttempts;

}
