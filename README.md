# FinancialEntityService

Gateway de agregación y orquestación para el sistema bancario. Consolida las solicitudes de préstamos de múltiples entidades financieras (Bancolombia, Davivienda, Coltefinanciera) y proporciona simulación de crédito con validación de políticas a través de n8n.

## 🚀 Ejecución

### Modo Desarrollo (Test)

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=test"
```

**Características:**

- ✅ Sin autenticación JWT
- ✅ Logs en modo DEBUG
- ✅ Circuit breakers habilitados
- ✅ Webhooks n8n en localhost

**Acceso:**

- Gateway: http://localhost:8080
- Health: http://localhost:8080/actuator/health
- Circuit Breakers: http://localhost:8080/actuator/circuitbreakers

---

### Modo Producción

```bash
mvn spring-boot:run
```

**Características:**

- ✅ Autenticación JWT con Clerk (si se configura)
- ✅ Logs en modo INFO
- ✅ Resilience4j con circuit breakers
- ✅ Webhooks n8n en URL configurada

**Requiere configurar variables de entorno:**

```bash
export N8N_SIMULATION_URL=https://your-n8n-server.com/webhook/credit-simulation
```

---

## 📦 Build y Despliegue

### Compilar el proyecto

```bash
mvn clean package
```

### Ejecutar JAR

```bash
java -jar target/FinancialEntityService-0.0.1-SNAPSHOT.jar
```

### Ejecutar con perfil específico

```bash
java -jar target/FinancialEntityService-0.0.1-SNAPSHOT.jar --spring.profiles.active=test
```

---

## 🔧 Configuración

### Variables de Entorno

| Variable              | Descripción                         | Valor por Defecto                                 |
| --------------------- | ----------------------------------- | ------------------------------------------------- |
| `N8N_SIMULATION_URL`  | URL del webhook n8n para simulación | `http://localhost:5678/webhook/credit-simulation` |
| `BANCOLOMBIA_URL`     | URL del servicio Bancolombia        | `http://localhost:8083`                           |
| `DAVIVIENDA_URL`      | URL del servicio Davivienda         | `http://localhost:8082`                           |
| `COLTEFINANCIERA_URL` | URL del servicio Coltefinanciera    | `http://localhost:8081`                           |

### Archivo application.yaml

```yaml
server:
  port: 8080

resilience4j:
  circuitbreaker:
    instances:
      bancolombia:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s

n8n:
  webhook:
    simulation-url: ${N8N_SIMULATION_URL:http://localhost:5678/webhook/credit-simulation}
```

---

## 🧪 Pruebas

### Ejecutar tests

```bash
mvn test
```

### Ejecutar tests con coverage

```bash
mvn clean verify
```

---

## 📊 Endpoints Principales

| Método | Endpoint                          | Descripción                                       |
| ------ | --------------------------------- | ------------------------------------------------- |
| `GET`  | `/api/applications/user/{userId}` | Obtener solicitudes agregadas de todos los bancos |
| `POST` | `/api/simulation`                 | Simular crédito con validación de políticas (n8n) |
| `GET`  | `/actuator/health`                | Estado del servicio                               |
| `GET`  | `/actuator/circuitbreakers`       | Estado de circuit breakers                        |

---

## 🐳 Docker

### Build imagen

```bash
docker build -t financial-gateway:latest .
```

### Ejecutar contenedor

```bash
docker run -p 8080:8080 \
  -e N8N_SIMULATION_URL=https://n8n-server.com/webhook/credit-simulation \
  financial-gateway:latest
```

---

## 📝 Dependencias

- Spring Boot 3.5.7
- Spring Cloud Gateway
- Resilience4j (Circuit Breaker)
- Spring WebFlux (Cliente reactivo)
- Lombok

---

## 🔗 Servicios Relacionados

- **Bancolombia Service**: http://localhost:8083
- **Davivienda Service**: http://localhost:8082
- **Coltefinanciera Service**: http://localhost:8081
- **n8n Workflow**: http://localhost:5678


