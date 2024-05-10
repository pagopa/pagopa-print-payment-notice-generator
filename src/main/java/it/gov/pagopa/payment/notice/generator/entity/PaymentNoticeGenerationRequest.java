package it.gov.pagopa.payment.notice.generator.entity;

import it.gov.pagopa.payment.notice.generator.model.enums.PaymentGenerationRequestStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Document("payment_notice_generation_request")
@ToString
public class PaymentNoticeGenerationRequest {

    @Id
    private String id;

    @Indexed()
    private String userId;

    @CreatedDate
    private Instant createdAt;

    private Instant requestDate;

    private PaymentGenerationRequestStatus status;

    private List<String> items;

    private Integer numberOfElementsProcessed;

    private Integer numberOfElementsFailed;

    private Integer numberOfElementsTotal;

}
