package art.snail.naillian.backend.domain.user.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table("user")
public class User {
    @Id
    private Integer id;
    private String nickname;
    private String userType;
    private String profileImageUrl;
    private String registeredIp;

    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("deleted_at")
    private LocalDateTime deletedAt;
}
