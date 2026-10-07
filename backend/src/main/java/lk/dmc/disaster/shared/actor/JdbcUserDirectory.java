package lk.dmc.disaster.shared.actor;

import java.util.Optional;
import java.util.UUID;
import lk.dmc.disaster.shared.domain.Role;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class JdbcUserDirectory implements UserDirectory {

  private final JdbcClient jdbc;

  JdbcUserDirectory(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<UserSummary> find(UUID userId) {
    return jdbc.sql("select id, full_name, role from users where id = :id")
        .param("id", userId)
        .query(
            (rs, row) ->
                new UserSummary(
                    rs.getObject("id", UUID.class),
                    rs.getString("full_name"),
                    Role.valueOf(rs.getString("role"))))
        .optional();
  }
}
