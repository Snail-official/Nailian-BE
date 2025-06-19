package art.snail.naillian.backend.page;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.repository.query.R2dbcParameterAccessor;
import org.springframework.data.r2dbc.repository.query.R2dbcQueryMethod;
import org.springframework.lang.NonNull;

public class CursorSupportingR2dbcParameterAccessor extends R2dbcParameterAccessor {
    private static final Logger log = LoggerFactory.getLogger(CursorSupportingR2dbcParameterAccessor.class);

    public CursorSupportingR2dbcParameterAccessor(R2dbcQueryMethod method, Object... values) {
        super(method, values);
    }

    @Override
    @NonNull
    public Sort getSort() {
        if (getParameters().hasPageableParameter()) {
            Pageable pageable = (Pageable) getValues()[getParameters().getPageableIndex()];

            if (pageable instanceof CursorPageRequest) {
                log.info("CursorPageRequest found!");
            }
        }

        return super.getSort();
    }
}
