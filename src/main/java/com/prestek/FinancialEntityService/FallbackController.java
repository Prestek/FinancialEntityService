package com.prestek.FinancialEntityService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @GetMapping
    public ResponseEntity<Map<String, Object>> fallback(
            @RequestHeader("X-Bank-Code") String bankCode) {

        log.warn("Fallback activado para banco: {}", bankCode);

        Map<String, Object> body = Map.of(
                "error", "Servicio temporalmente no disponible",
                "bank", bankCode,
                "timestamp", Instant.now(),
                "retryAfter", "60");

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "60")
                .body(body);
    }
}
