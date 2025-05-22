package com.esvar.dekanat.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
public class WebConfig {
//    @Bean
//    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
//        FilterRegistrationBean<ForwardedHeaderFilter> bean =
//                new FilterRegistrationBean<>(new ForwardedHeaderFilter());
//        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
//        return bean;
//    }
}
