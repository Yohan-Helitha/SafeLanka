package lk.dmc.disaster.shared.config;

import lk.dmc.disaster.shared.sms.SmsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the SMS settings; the matching gateway implementation activates itself from them. */
@Configuration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsConfig {}
