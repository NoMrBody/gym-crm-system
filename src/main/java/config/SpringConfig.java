package config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan(basePackages = {"dao", "service", "facade", "storage", "config"})
@PropertySource("classpath:application.properties")
public class SpringConfig {

}