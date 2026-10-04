package lk.dmc.disaster;

import org.springframework.boot.SpringApplication;

public class TestDisasterApplication {

  public static void main(String[] args) {
    SpringApplication.from(DisasterApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
