package com.hrutvik.orders;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import static org.assertj.core.api.Assertions.assertThat;

class DeploymentConfigurationTest {
  @Test void enablesGracefulShutdownAndAvailabilityProbes(){
    var yaml=new YamlPropertiesFactoryBean();yaml.setResources(new ClassPathResource("application.yml"));var properties=yaml.getObject();
    assertThat(properties).isNotNull();
    assertThat(properties.getProperty("server.shutdown")).isEqualTo("graceful");
    assertThat(properties.getProperty("spring.lifecycle.timeout-per-shutdown-phase")).isEqualTo("30s");
    assertThat(properties.getProperty("management.endpoint.health.probes.enabled")).isEqualTo("true");
    assertThat(properties.getProperty("management.health.livenessstate.enabled")).isEqualTo("true");
    assertThat(properties.getProperty("management.health.readinessstate.enabled")).isEqualTo("true");
  }
}
