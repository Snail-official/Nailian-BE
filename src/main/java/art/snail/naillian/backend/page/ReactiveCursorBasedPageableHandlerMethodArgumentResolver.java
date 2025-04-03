package art.snail.naillian.backend.page;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolverSupport;
import org.springframework.data.web.ReactiveSortHandlerMethodArgumentResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

public class ReactiveCursorBasedPageableHandlerMethodArgumentResolver extends PageableHandlerMethodArgumentResolverSupport implements SyncHandlerMethodArgumentResolver {
    private static final Logger log = LoggerFactory.getLogger(ReactiveCursorBasedPageableHandlerMethodArgumentResolver.class);
    private final ReactiveSortHandlerMethodArgumentResolver sortResolver = new ReactiveSortHandlerMethodArgumentResolver();

    @Override
    @Nullable
    public Pageable resolveArgumentValue(MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {
        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
        String page = queryParams.getFirst(this.getParameterNameToUse(this.getPageParameterName(), parameter));
        String pageSize = queryParams.getFirst(this.getParameterNameToUse(this.getSizeParameterName(), parameter));
        String cursor = queryParams.getFirst(this.getParameterNameToUse("cursor", parameter));
        Sort sort = this.sortResolver.resolveArgumentValue(parameter, bindingContext, exchange);

        boolean isPagePresent = page != null && !page.isEmpty();
        boolean isCursorPresent = cursor != null && !cursor.isEmpty();

        Pageable pageable = this.getPageable(parameter, page, pageSize);
        if (isCursorPresent) {
            if (isPagePresent) log.warn("both cursor and page are present simultaneously.");
            return pageable.isPaged()
                    ? CursorPageRequest.of(pageable.getPageSize(), sort, cursor)
                    : Pageable.unpaged(sort);
        } else if (!sort.isSorted()) {
            return pageable;
        }

        return pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)
                : Pageable.unpaged(sort);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Pageable.class.equals(parameter.getParameterType());
    }
}
