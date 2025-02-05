package art.snail.naillian.backend.attributes;

import art.snail.naillian.backend.errors.ReportableError;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

/**
 * 전역에서 발생하는 Exception 의 유형에 따라 body 를 재정의하기 위한
 */
@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {
    private static final Logger log = LogManager.getLogger(GlobalErrorAttributes.class);

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(request, options);

        Throwable ex = getError(request);
        if (ex instanceof ReportableError) {
            attributes.clear();
            attributes.put("code", ((ReportableError) ex).getStatus().value());
            attributes.put("message", ex.getMessage());
        } else {
            log.warn("Unhandled error", ex);
        }

        return attributes;
    }
}
