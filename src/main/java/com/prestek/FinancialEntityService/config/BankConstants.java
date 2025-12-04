package com.prestek.FinancialEntityService.config;

public final class BankConstants {

    private BankConstants() {
    }

    public enum BankService {
        BANCOLOMBIA(
                System.getProperty("BANCOLOMBIA_SERVICE_URL", "http://localhost:8083"),
                "Authorization", "Bancolombia", "BCO"),
        DAVIVIENDA(
                System.getProperty("DAVIVIENDA_SERVICE_URL", "http://localhost:8082"),
                "Authorization", "Davivienda", "DAVI"),
        COLTEFINANCIERA(
                System.getProperty("COLTEFINANCIERA_SERVICE_URL", "http://localhost:8081"),
                "Authorization", "Coltefinanciera", "COLT");

        private final String baseUrl;
        private final String authHeader;
        private final String bankName;
        private final String bankCode;

        BankService(String baseUrl, String authHeader, String bankName, String bankCode) {
            this.baseUrl = baseUrl;
            this.authHeader = authHeader;
            this.bankName = bankName;
            this.bankCode = bankCode;
        }

        public String buildUri(String path) {
            return this.baseUrl + path;
        }

        public String authHeader() {
            return authHeader;
        }

        public String bankName() {
            return bankName;
        }

        public String bankCode() {
            return bankCode;
        }
    }

    public enum BankPaths {
        GET_APPLICATIONS_BY_USER("/api/applications/user/%s");

        private final String path;

        BankPaths(String path) {
            this.path = path;
        }

        public String format(String userId) {
            return path.formatted(userId);
        }
    }
}
