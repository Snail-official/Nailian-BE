package art.snail.naillian.backend.page;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.NonNull;
import org.springframework.r2dbc.core.DatabaseClient;

import java.io.Serializable;

public class CursorSupportingR2dbcRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
        extends R2dbcRepositoryFactoryBean<T, S, ID> {

    /**
     * Creates a new {@link R2dbcRepositoryFactoryBean} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    public CursorSupportingR2dbcRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    @NonNull
    protected RepositoryFactorySupport getFactoryInstance(
            @NonNull DatabaseClient client,
            @NonNull ReactiveDataAccessStrategy dataAccessStrategy
    ) {
        return new CursorSupportingR2dbcDatabaseFactory(client, dataAccessStrategy);
    }

    @Override
    @NonNull
    protected RepositoryFactorySupport getFactoryInstance(
            @NonNull R2dbcEntityOperations operations
    ) {
        return new CursorSupportingR2dbcDatabaseFactory(operations);
    }

    @Override
    public void setEntityOperations(R2dbcEntityOperations operations) {
        super.setEntityOperations(operations);
    }
}
