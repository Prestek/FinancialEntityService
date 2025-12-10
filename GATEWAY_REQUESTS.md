# Guía de Solicitudes a través del Gateway

Este documento muestra cómo hacer peticiones a los diferentes bancos a través del **Financial Entity Gateway** (puerto 8080).

## 🔧 Configuración

**Gateway URL**: `http://localhost:8080` (desarrollo) o tu URL de producción

### Headers Requeridos

El Gateway usa el header `X-Bank-Code` para enrutar las peticiones:

- **Bancolombia**: `X-Bank-Code: BCO`
- **Davivienda**: `X-Bank-Code: DAVI`
- **Coltefinanciera**: `X-Bank-Code: COLT`

---

## 📋 Endpoints Disponibles

### 1. **Obtener todas las solicitudes**

```http
GET {{gateway_url}}/api/applications
X-Bank-Code: BCO
```

**Ejemplo con curl:**

```bash
curl -X GET http://localhost:8080/api/applications \
  -H "X-Bank-Code: BCO"
```

---

### 2. **Obtener solicitud por ID**

```http
GET {{gateway_url}}/api/applications/{id}
X-Bank-Code: DAVI
```

**Ejemplo:**

```bash
curl -X GET http://localhost:8080/api/applications/1 \
  -H "X-Bank-Code: DAVI"
```

---

### 3. **Obtener solicitudes por Usuario**

```http
GET {{gateway_url}}/api/applications/user/{userId}
X-Bank-Code: COLT
```

**Ejemplo:**

```bash
curl -X GET http://localhost:8080/api/applications/user/USR001 \
  -H "X-Bank-Code: COLT"
```

---

### 4. **Obtener solicitudes por Estado**

```http
GET {{gateway_url}}/api/applications/status/{status}
X-Bank-Code: BCO
```

**Estados válidos**: `PENDING`, `APPROVED`, `REJECTED`, `WITHDRAWN`

**Ejemplo:**

```bash
curl -X GET http://localhost:8080/api/applications/status/PENDING \
  -H "X-Bank-Code: BCO"
```

---

### 5. **Crear nueva solicitud**

```http
POST {{gateway_url}}/api/applications
X-Bank-Code: DAVI
Content-Type: application/json

{
  "userId": "USR001",
  "amount": 15000000
}
```

**Ejemplo con curl:**

```bash
curl -X POST http://localhost:8080/api/applications \
  -H "X-Bank-Code: DAVI" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USR001",
    "amount": 15000000
  }'
```

---

### 6. **Actualizar estado de solicitud**

```http
PATCH {{gateway_url}}/api/applications/{id}/status
X-Bank-Code: BCO
Content-Type: application/json

{
  "status": "APPROVED",
  "notes": "Application approved after review"
}
```

**Ejemplo:**

```bash
curl -X PATCH http://localhost:8080/api/applications/1/status \
  -H "X-Bank-Code: BCO" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "APPROVED",
    "notes": "Application approved after review"
  }'
```

---

### 7. **Obtener cantidad de solicitudes por usuario**

```http
GET {{gateway_url}}/api/applications/user/{userId}/count
X-Bank-Code: COLT
```

**Ejemplo:**

```bash
curl -X GET http://localhost:8080/api/applications/user/USR001/count \
  -H "X-Bank-Code: COLT"
```

**Respuesta:**

```json
{
  "count": 5
}
```

---

### 8. **Eliminar solicitud**

```http
DELETE {{gateway_url}}/api/applications/{id}
X-Bank-Code: DAVI
```

**Ejemplo:**

```bash
curl -X DELETE http://localhost:8080/api/applications/1 \
  -H "X-Bank-Code: DAVI"
```

---

## 🌐 Endpoints Especiales del Gateway

### 9. **Simulación de Crédito (a través de N8N)**

```http
POST {{gateway_url}}/api/simulation
Content-Type: application/json

{
  "userId": "USR001",
  "amount": 20000000,
  "termMonths": 48,
  "monthlyIncome": 5000000
}
```

**Ejemplo:**

```bash
curl -X POST http://localhost:8080/api/simulation \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USR001",
    "amount": 20000000,
    "termMonths": 48,
    "monthlyIncome": 5000000
  }'
```

**Respuesta esperada:**

```json
{
  "success": true,
  "creditScore": 750,
  "requestedAmount": 20000000,
  "termMonths": 48,
  "monthlyIncome": 5000000,
  "bestOffer": {
    "entity": "Bancolombia",
    "approved": true,
    "effectiveAnnualRate": 12.5,
    "monthlyPayment": 520000,
    "totalCost": 24960000
  },
  "allOffers": [...],
  "offersCount": 3
}
```

---

### 10. **Agregación de solicitudes de todos los bancos**

```http
GET {{gateway_url}}/api/applications/user/{userId}
Authorization: Bearer {jwt_token}
```

Este endpoint **NO requiere** `X-Bank-Code` porque consulta automáticamente los 3 bancos.

**Ejemplo:**

```bash
curl -X GET http://localhost:8080/api/applications/user/USR001 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Respuesta esperada:**

```json
[
  {
    "id": 1,
    "userId": "USR001",
    "amount": 15000000,
    "status": "APPROVED",
    "bankName": "Bancolombia",
    ...
  },
  {
    "id": 5,
    "userId": "USR001",
    "amount": 10000000,
    "status": "PENDING",
    "bankName": "Davivienda",
    ...
  }
]
```

---

## 🎯 Ejemplos de Uso con Postman

### Variables de Entorno Sugeridas

```json
{
  "gateway_url": "http://localhost:8080",
  "bank_code_bancolombia": "BCO",
  "bank_code_davivienda": "DAVI",
  "bank_code_coltefinanciera": "COLT",
  "test_user_id": "USR001"
}
```

### Colección de Ejemplo

#### Request 1: Crear solicitud en Bancolombia

```
POST {{gateway_url}}/api/applications
Headers:
  X-Bank-Code: {{bank_code_bancolombia}}
  Content-Type: application/json
Body:
{
  "userId": "{{test_user_id}}",
  "amount": 20000000
}
```

#### Request 2: Consultar solicitudes del usuario en todos los bancos

```
GET {{gateway_url}}/api/applications/user/{{test_user_id}}
Headers:
  X-Bank-Code: {{bank_code_bancolombia}}

GET {{gateway_url}}/api/applications/user/{{test_user_id}}
Headers:
  X-Bank-Code: {{bank_code_davivienda}}

GET {{gateway_url}}/api/applications/user/{{test_user_id}}
Headers:
  X-Bank-Code: {{bank_code_coltefinanciera}}
```

#### Request 3: Simular crédito

```
POST {{gateway_url}}/api/simulation
Headers:
  Content-Type: application/json
Body:
{
  "userId": "{{test_user_id}}",
  "amount": 20000000,
  "termMonths": 48,
  "monthlyIncome": 5000000
}
```

---

## ⚡ Circuit Breaker (Resiliencia)

El Gateway tiene configurado **Circuit Breaker** con Resilience4j:

- **Ventana deslizante**: 10 llamadas
- **Mínimo de llamadas**: 5
- **Umbral de falla**: 50%
- **Tiempo en estado abierto**: 30 segundos

### Endpoints de Fallback

Si un banco no responde, el Gateway retorna:

```json
{
  "timestamp": "2025-11-30T10:30:00",
  "status": 503,
  "error": "Service Unavailable",
  "message": "El servicio del banco no está disponible temporalmente",
  "path": "/api/applications"
}
```

---

## 🔍 Health Check

Verificar estado del Gateway y Circuit Breakers:

```bash
curl http://localhost:8080/actuator/health
```

**Respuesta:**

```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "bancolombiaCB": "CLOSED",
        "daviviendaCB": "CLOSED",
        "coltefinancieraCB": "OPEN"
      }
    }
  }
}
```

---

## 📊 Métricas

Consultar métricas de Circuit Breakers:

```bash
curl http://localhost:8080/actuator/circuitbreakers
```

---

## 🚨 Manejo de Errores

### Error 404 - Banco no configurado

```json
{
  "timestamp": "2025-11-30T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "No route found for X-Bank-Code: INVALID"
}
```

### Error 400 - Header faltante

Si no se envía `X-Bank-Code` en endpoints que lo requieren:

```json
{
  "timestamp": "2025-11-30T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Required header 'X-Bank-Code' is not present"
}
```

---

## 🔐 Autenticación (Próximamente)

Algunos endpoints requerirán JWT token en el header `Authorization`:

```http
Authorization: Bearer {jwt_token}
```

---

## 📝 Notas Importantes

1. **Circuit Breaker**: Si un banco falla repetidamente, el Gateway lo marcará como "OPEN" y devolverá fallback durante 30 segundos.

2. **Timeout**: Las peticiones tienen un timeout de 5 segundos por defecto.

3. **Retry**: No hay reintentos automáticos, pero puedes implementarlos en el cliente.

4. **CORS**: El Gateway acepta peticiones desde cualquier origen (`*`).

5. **Validación**: Cada banco valida sus propios datos. El Gateway solo enruta las peticiones.

---

## 🌍 URLs de Producción

**Gateway Production**: Configurar según tu despliegue

**Bancos en Producción**:

- Bancolombia: `http://35.172.89.140:8080`
- Davivienda: `http://52.2.175.22:8080`
- Coltefinanciera: `https://coltefinanciera.eci-pigball.online`

---

## 📞 Soporte

Para reportar problemas con el Gateway, revisa los logs:

```bash
tail -f logs/gateway.log
```

O consulta el actuator:

```bash
curl http://localhost:8080/actuator/info
```
