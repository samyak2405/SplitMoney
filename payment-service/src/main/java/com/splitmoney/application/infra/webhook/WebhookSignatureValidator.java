package com.splitmoney.application.infra.webhook;

import com.splitmoney.application.config.HyperswitchProperties;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class WebhookSignatureValidator {

    private final HyperswitchProperties hyperswitchProperties;

    public WebhookSignatureValidator(HyperswitchProperties hyperswitchProperties) {
        this.hyperswitchProperties = hyperswitchProperties;
    }

    public boolean isValid(String payload, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    hyperswitchProperties.webhookSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return expected.equalsIgnoreCase(signature);
        } catch (Exception ex) {
            return false;
        }
    }
}
