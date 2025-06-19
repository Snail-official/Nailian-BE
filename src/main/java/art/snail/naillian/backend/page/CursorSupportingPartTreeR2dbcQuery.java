package art.snail.naillian.backend.page;

import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.repository.query.PartTreeR2dbcQuery;
import org.springframework.data.r2dbc.repository.query.R2dbcParameterAccessor;
import org.springframework.data.r2dbc.repository.query.R2dbcQueryMethod;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

public class CursorSupportingPartTreeR2dbcQuery extends PartTreeR2dbcQuery {
    public CursorSupportingPartTreeR2dbcQuery(R2dbcQueryMethod method, R2dbcEntityOperations entityOperations, R2dbcConverter converter, ReactiveDataAccessStrategy dataAccessStrategy) {
        super(method, entityOperations, converter, dataAccessStrategy);
    }

    @Override
    @NonNull
    public Object execute(@NonNull Object[] parameters) {
        Mono<R2dbcParameterAccessor> resolveParameters = new CursorSupportingR2dbcParameterAccessor(getQueryMethod(), parameters).resolveParameters();
        return resolveParameters.flatMapMany(it -> createQuery(it).flatMapMany(foo -> executeQuery(it, foo)));
    }
}
