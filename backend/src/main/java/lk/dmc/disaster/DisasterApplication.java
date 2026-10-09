package lk.dmc.disaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DisasterApplication {

  public static void main(String[] args) {
    SpringApplication.run(DisasterApplication.class, args);
  }
}
