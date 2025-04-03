package edu.kangwon.university.taxicarpool.map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MapConfig {
    
    @Bean
    public RestTemplate mapRestTemplate() {
        return new RestTemplate();
    }

}
