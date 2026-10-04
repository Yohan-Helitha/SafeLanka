package lk.dmc.disaster.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables JPA auditing for {@code @CreatedDate} and {@code @LastModifiedDate} fields. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
