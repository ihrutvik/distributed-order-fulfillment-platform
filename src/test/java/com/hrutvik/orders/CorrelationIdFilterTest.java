package com.hrutvik.orders;

import com.hrutvik.orders.observability.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {
  @Test void preservesSafeClientCorrelationId()throws Exception{
    var request=new MockHttpServletRequest();request.addHeader(CorrelationIdFilter.HEADER,"checkout-42");var response=new MockHttpServletResponse();
    new CorrelationIdFilter().doFilter(request,response,(req,res)->{});
    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("checkout-42");
  }
  @Test void replacesUnsafeCorrelationId()throws Exception{
    var request=new MockHttpServletRequest();request.addHeader(CorrelationIdFilter.HEADER,"bad\nvalue");var response=new MockHttpServletResponse();
    new CorrelationIdFilter().doFilter(request,response,(req,res)->{});
    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).matches("[0-9a-f-]{36}");
  }
}
