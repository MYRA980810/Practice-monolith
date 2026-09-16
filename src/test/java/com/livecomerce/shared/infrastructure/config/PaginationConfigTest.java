package com.livecomerce.shared.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for the global paginate-anything cap used by every {@code Pageable}
 * controller parameter in the app (e.g. GET /api/products/browse,
 * GET /api/products/{id}/reviews). Exercises the real
 * {@link PageableHandlerMethodArgumentResolverCustomizer} bean directly against
 * Spring's resolver — no Spring context needed — to confirm a client asking for
 * a page bigger than {@link PaginationConfig#MAX_PAGE_SIZE} is capped rather
 * than honored.
 */
class PaginationConfigTest {

    @Test
    void pageableCustomizer_capsOversizedPageRequestAtMaxPageSize() throws Exception {
        var resolver = new PageableHandlerMethodArgumentResolver();
        new PaginationConfig().pageableCustomizer().customize(resolver);

        var request = new MockHttpServletRequest();
        request.addParameter("page", "0");
        request.addParameter("size", "500");
        var webRequest = new ServletWebRequest(request);

        Pageable pageable = (Pageable) resolver.resolveArgument(pageableParameter(), null, webRequest, null);

        assertThat(pageable.getPageSize()).isEqualTo(100);
    }

    @Test
    void pageableCustomizer_leavesRequestsUnderTheCapUnchanged() throws Exception {
        var resolver = new PageableHandlerMethodArgumentResolver();
        new PaginationConfig().pageableCustomizer().customize(resolver);

        var request = new MockHttpServletRequest();
        request.addParameter("page", "1");
        request.addParameter("size", "20");
        var webRequest = new ServletWebRequest(request);

        Pageable pageable = (Pageable) resolver.resolveArgument(pageableParameter(), null, webRequest, null);

        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getPageNumber()).isEqualTo(1);
    }

    private static MethodParameter pageableParameter() throws NoSuchMethodException {
        Method dummy = PaginationConfigTest.class.getDeclaredMethod("dummyEndpoint", Pageable.class);
        return new MethodParameter(dummy, 0);
    }

    @SuppressWarnings("unused")
    private void dummyEndpoint(Pageable pageable) {
        // signature-only, used to obtain a MethodParameter for the resolver
    }
}
