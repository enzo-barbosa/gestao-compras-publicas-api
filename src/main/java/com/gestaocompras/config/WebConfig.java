package com.gestaocompras.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver pageableResolver =
                new PageableHandlerMethodArgumentResolver();
        pageableResolver.setMaxPageSize(MAX_PAGE_SIZE);
        resolvers.add(pageableResolver);
    }
}
