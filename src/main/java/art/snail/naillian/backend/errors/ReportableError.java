package art.snail.naillian.backend.errors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 이용자에게 오류 응답을 전송할 수 있는 RuntimeException
 *
 * @see art.snail.naillian.backend.attributes.GlobalErrorAttributes
 * @see art.snail.naillian.backend.errors.GlobalErrorWebExceptionHandler
 */
@Getter
@AllArgsConstructor
public class ReportableError extends RuntimeException {
    private final HttpStatus status;
    private final String message;
    private final Object data;

    public ReportableError(HttpStatus status) {
        this(status, status.getReasonPhrase(), null);
    }

    public ReportableError(HttpStatus status, String message) {
        this(status, message, null);
    }
}
