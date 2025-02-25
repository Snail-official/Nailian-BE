package art.snail.naillian.backend.common;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.NonNull;

import java.io.IOException;


@Builder
@JsonSerialize(using = CommonResponse.Serializer.class)
public class CommonResponse<T> {
    @Builder.Default
    private int code = 200;

    @Builder.Default
    private String msg = "";

    @Builder.Default
    private T data = null;

    public static <T> @NonNull CommonResponse<T> success(T data) {
        return CommonResponse.<T>builder()
                .data(data)
                .build();
    }

    public static <T> @NonNull CommonResponse<T> fail(HttpStatusCode code, String message) {
        return CommonResponse.<T>builder()
                .code(code.value())
                .msg(message)
                .build();
    }
    @JsonComponent
    static class Serializer extends JsonSerializer<CommonResponse<?>> {
        @Override
        public void serialize(CommonResponse<?> response, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
            gen.writeStartObject();
            gen.writeNumberField("code", response.code);
            gen.writeStringField("message", response.msg);
            gen.writeObjectField("data", response.data);
            gen.writeEndObject();
        }
    }
}
