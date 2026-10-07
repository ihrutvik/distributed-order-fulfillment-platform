package com.hrutvik.orders;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import static org.assertj.core.api.Assertions.assertThat;

class TracingConfigurationTest {
  @Test void enablesKafkaObservationAndOtlpExport(){
    var yaml=new YamlPropertiesFactoryBean();yaml.setResources(new ClassPathResource("application.yml"));var properties=yaml.getObject();
    assertThat(properties).isNotNull();
    assertThat(properties.getProperty("spring.kafka.template.observation-enabled")).isEqualTo("true");
    assertThat(properties.getProperty("spring.kafka.listener.observation-enabled")).isEqualTo("true");
    assertThat(properties.getProperty("management.otlp.tracing.endpoint")).contains("4318/v1/traces");
    assertThat(properties.getProperty("logging.pattern.correlation")).contains("traceId").contains("spanId").contains("correlationId");
  }
}
