package art.snail.naillian.backend.domain.user.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("event_submission")
public class EventSubmission {
    @Id
    private Integer id;

    private Integer userId;
    private Integer nailSetId;

    private String email;
    private String phoneNumber;

    private LocalDateTime createdAt;
}
