package art.snail.naillian.backend.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

@JsonSerialize(using = CursorPageDTO.Serializer.class)
public class CursorPageDTO<T> extends PageImpl<T> {
    @Getter
    private final String cursor;

    public CursorPageDTO(String cursor, List<T> content, Pageable pageable, long total) {
        super(content, pageable, total);
        this.cursor = cursor;
    }

    @JsonComponent
    static class Serializer extends JsonSerializer<CursorPageDTO<?>> {

        @Override
        public void serialize(CursorPageDTO<?> page, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();

            // pageInfo
            {
                gen.writeFieldName("pageInfo");
                gen.writeStartObject();
                gen.writeNumberField("totalElements", page.getTotalElements());
                gen.writeNumberField("totalPages", page.getTotalPages());
                gen.writeStringField("cursor", page.getCursor());
                gen.writeEndObject();
            }

            // content[]
            {
                gen.writeFieldName("content");
                gen.writeStartArray();
                for (Object content : page.getContent()) {
                    gen.writeObject(content);
                }
                gen.writeEndArray();
            }

            gen.writeEndObject();
        }
    }
}
