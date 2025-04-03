package art.snail.naillian.backend.page;

import io.jsonwebtoken.lang.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

public class CursorPageRequest extends PageRequest {
    private static final Logger log = LoggerFactory.getLogger(CursorPageRequest.class);
    private final String cursor;

    protected CursorPageRequest(int pageNumber, int pageSize, Sort sort, @NonNull String cursor) {
        super(pageNumber, pageSize, sort);
        Assert.notNull(cursor, "Cursor must not be null");
        this.cursor = cursor;
    }

    public static CursorPageRequest of(int pageSize, Sort sort, String cursor) {
        return new CursorPageRequest(0, pageSize, sort, cursor);
    }

    @Override
    public int getPageNumber() {
        log.warn("Reading page number from CursorPagedRequest");
        return 0;
    }

    public String getCursor() {
        return cursor;
    }

    @Override
    public PageRequest next() {
        throw new RuntimeException("operation not supported");
    }

    @Override
    public PageRequest previous() {
        throw new RuntimeException("operation not supported");
    }

    @Override
    public PageRequest withPage(int pageNumber) {
        return this;
    }

    @Override
    public PageRequest withSort(Sort.Direction direction, String... properties) {
        return new CursorPageRequest(this.getPageNumber(), this.getPageSize(), Sort.by(direction, properties), this.getCursor());
    }

    @Override
    public PageRequest withSort(Sort sort) {
        return new CursorPageRequest(this.getPageNumber(), this.getPageSize(), sort, this.getCursor());
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + this.getPageSize();
        result = 31 * result + this.getCursor().hashCode();
        result = 31 * result + this.getSort().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("Cursor page request [cursor: %s, size %d, sort: %s]", this.cursor, this.getPageSize(), this.getSort());
    }
}
