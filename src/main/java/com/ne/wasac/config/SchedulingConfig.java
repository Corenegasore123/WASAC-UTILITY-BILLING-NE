package com.ne.wasac.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the automatic late-payment penalty cron job. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
