package art.snail.naillian.backend.page;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.repository.query.R2dbcQueryMethod;
import org.springframework.data.r2dbc.repository.query.StringBasedR2dbcQuery;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.CachingValueExpressionDelegate;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.lang.NonNull;
import org.springframework.r2dbc.core.DatabaseClient;

import java.lang.reflect.Method;
import java.util.Optional;

public class CursorSupportingR2dbcDatabaseFactory extends R2dbcRepositoryFactory {
    public CursorSupportingR2dbcDatabaseFactory(DatabaseClient databaseClient, ReactiveDataAccessStrategy dataAccessStrategy) {
        super(databaseClient, dataAccessStrategy);
    }

    public CursorSupportingR2dbcDatabaseFactory(R2dbcEntityOperations operations) {
        super(operations);
    }

    @Override
    @NonNull
    protected Object getTargetRepository(@NonNull RepositoryInformation information) {
        return super.getTargetRepository(information);
    }

    @Override
    @NonNull
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key, ValueExpressionDelegate valueExpressionDelegate) {
        return Optional.of(new CursorSupportingR2dbcQueryLookupStrategy(
                operations,
                new CachingValueExpressionDelegate(valueExpressionDelegate),
                converter,
                dataAccessStrategy
        ));
    }

    protected static class CursorSupportingR2dbcQueryLookupStrategy extends R2dbcQueryLookupStrategy {
        protected CursorSupportingR2dbcQueryLookupStrategy(R2dbcEntityOperations entityOperations, ValueExpressionDelegate delegate, R2dbcConverter converter, ReactiveDataAccessStrategy dataAccessStrategy) {
            super(entityOperations, delegate, converter, dataAccessStrategy);
        }

        @Override
        @NonNull
        public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory, NamedQueries namedQueries) {
            R2dbcQueryMethod queryMethod = new R2dbcQueryMethod(method, metadata, factory, getMappingContext());
            String namedQueryName = queryMethod.getNamedQueryName();

            if (namedQueries.hasQuery(namedQueryName) || queryMethod.hasAnnotatedQuery()) {

                String query = namedQueries.hasQuery(namedQueryName) ? namedQueries.getQuery(namedQueryName)
                        : queryMethod.getRequiredAnnotatedQuery();
                query = evaluateTableExpressions(metadata, query);

                return new StringBasedR2dbcQuery(query, queryMethod, this.entityOperations, this.converter, this.dataAccessStrategy, this.delegate);

            } else {
                return new CursorSupportingPartTreeR2dbcQuery(queryMethod, this.entityOperations, this.converter, this.dataAccessStrategy);
            }
        }
    }
}
