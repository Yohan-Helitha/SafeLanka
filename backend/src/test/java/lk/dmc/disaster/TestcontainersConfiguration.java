package lk.dmc.disaster;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

  // Pass -Dapp.test.use-testcontainers=false to test against a local PostgreSQL instead of Docker.
  @Bean
  @ServiceConnection
  @ConditionalOnProperty(
      name = "app.test.use-testcontainers",
      havingValue = "true",
      matchIfMissing = true)
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
  }
}
