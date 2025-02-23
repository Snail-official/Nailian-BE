package art.snail.naillian.backend.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

@JsonSerialize(using = PageDTO.Serializer.class)
public class PageDTO<T> extends PageImpl<T> {
    public PageDTO(List<T> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    @JsonComponent
    @SuppressWarnings("unchecked")
    static class Serializer extends JsonSerializer<PageDTO<?>> {
        @Override
        public void serialize(PageDTO page, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
            gen.writeStartObject();

            gen.writeNumberField("page", page.getNumber());
            gen.writeNumberField("size", page.getSize());
            gen.writeNumberField("totalElements", page.getTotalElements());
            gen.writeNumberField("totalPages", page.getTotalPages());

            gen.writeFieldName("content");
            gen.writeStartArray();
            for (Object content : page.getContent()) {
                gen.writeObject(content);
            }
            gen.writeEndArray();

            gen.writeEndObject();
        }
    }
}
