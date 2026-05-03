package com.splitmoney.ai.observability;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "langfuse")
public class LangfuseProperties {
    private boolean enabled = true;
    private String baseUrl = "http://localhost:3002";
    private String publicKey = "";
    private String secretKey = "";
}
