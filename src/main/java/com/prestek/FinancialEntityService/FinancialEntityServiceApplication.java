package com.prestek.FinancialEntityService;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class FinancialEntityServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(FinancialEntityServiceApplication.class);

    public static void main(String[] args) {
        // Cargar .env automáticamente ANTES de iniciar Spring
        try {
            logger.info("════════════════════════════════════════════");
            logger.info("🔍 Buscando archivo .env...");

            // Obtener directorio actual
            String currentDir = System.getProperty("user.dir");
            logger.info("   Directorio actual: {}", currentDir);

            Dotenv dotenv = Dotenv.configure()
                    .directory(currentDir)
                    .ignoreIfMissing()
                    .load();

            if (dotenv.entries().isEmpty()) {
                logger.warn("⚠️  Archivo .env no encontrado o está vacío");
                logger.warn("   Las aplicaciones usarán valores por defecto (localhost)");
            } else {
                logger.info("✅ Archivo .env encontrado con {} variables", dotenv.entries().size());

                // Setear como system properties para que Spring las use
                dotenv.entries().forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    System.setProperty(key, value);
                    logger.info("   ✓ {} = {}", key,
                            value.length() > 50 ? value.substring(0, 47) + "..." : value);
                });
            }

            logger.info("════════════════════════════════════════════");
        } catch (Exception e) {
            logger.error("❌ Error cargando .env: {}", e.getMessage(), e);
        }

        SpringApplication app = new SpringApplication(FinancialEntityServiceApplication.class);
        app.run(args);
    }

    @Component
    static class StartupLogger {

        private static final Logger logger = LoggerFactory.getLogger(StartupLogger.class);

        @Value("${BANCOLOMBIA_SERVICE_URL:http://localhost:8083}")
        private String bancolombiaUrl;

        @Value("${DAVIVIENDA_SERVICE_URL:http://localhost:8082}")
        private String daviviendaUrl;

        @Value("${COLTEFINANCIERA_SERVICE_URL:http://localhost:8081}")
        private String coltefinancieraUrl;

        @Value("${N8N_SIMULATION_URL:http://localhost:5678/webhook-test/simulate-credit}")
        private String n8nSimulationUrl;

        @EventListener(ApplicationReadyEvent.class)
        public void onApplicationReady() {
            logger.info("═══════════════════════════════════════════════════════");
            logger.info("🏦 BANCOS CONFIGURADOS:");
            logger.info("   Bancolombia     → {}", bancolombiaUrl);
            logger.info("   Davivienda      → {}", daviviendaUrl);
            logger.info("   Coltefinanciera → {}", coltefinancieraUrl);
            logger.info("");
            logger.info("🔗 SERVICIOS EXTERNOS:");
            logger.info("   N8N Simulation  → {}", n8nSimulationUrl);
            logger.info("═══════════════════════════════════════════════════════");
        }
    }
}