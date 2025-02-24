package art.snail.naillian.backend.domain.nail.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("nail_folder_set")
public class NailFolderSet {

    @Id
    private Integer id;
    private Integer folderId;
    private Integer setId;
    private LocalDateTime createdAt;
}

