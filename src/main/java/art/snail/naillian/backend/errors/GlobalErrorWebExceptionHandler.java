package art.snail.naillian.backend.errors;

import art.snail.naillian.backend.attributes.GlobalErrorAttributes;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 전역에서 발생하는 오류를 커스텀하고 ServerResponse 를 생성하는 Handler
 */
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {
    private static final HttpStatusCode DEFAULT_CODE = HttpStatus.BAD_GATEWAY;

    public GlobalErrorWebExceptionHandler(
            GlobalErrorAttributes errorAttributes,
            ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer
    ) {
        super(errorAttributes, new WebProperties.Resources(), applicationContext);
        super.setMessageReaders(serverCodecConfigurer.getReaders());
        super.setMessageWriters(serverCodecConfigurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        // 전역의 오류를 catch 함
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    /**
     * 오류로 생성된 Map 데이터를 JSON 으로 변환함
     */
    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorProperties = getErrorAttributes(request, ErrorAttributeOptions.defaults());

        Object rawCode = null;
        String message = null;

        if (errorProperties.containsKey("code")) {
            rawCode = errorProperties.get("code");
        } else if (errorProperties.containsKey("status")) {
            rawCode = errorProperties.remove("status");
        }
        if (errorProperties.containsKey("error")) {
            message = errorProperties.remove("error").toString();
        }

        HttpStatusCode code = DEFAULT_CODE;
        if (rawCode != null) {
            if (rawCode instanceof Number) {
                code = HttpStatus.valueOf((Integer) rawCode);
            } else if (rawCode instanceof HttpStatusCode) {
                code = (HttpStatusCode) rawCode;
            }
        }

        Optional<Object> exception = request.attribute("ExceptionHandlingWebHandler.handledException");
        if (exception.isPresent()) {
            Throwable handledException = (Throwable) exception.get();

            if (!(handledException instanceof ReportableError)) {
                String exceptionMessage = handledException.getMessage();
                if (exceptionMessage != null && !exceptionMessage.isEmpty()) {
                    message = exceptionMessage;
                }

                Map<String, Object> originalProperties = errorProperties;
                errorProperties = new HashMap<>();
                errorProperties.put("code", code.value());
                errorProperties.put("message", message);
                errorProperties.put("error", originalProperties);
            }
        }

        return ServerResponse.status(code)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorProperties));
    }
}
