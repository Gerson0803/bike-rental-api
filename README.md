# Bike Rental API

API REST para el alquiler de bicicletas urbanas. Desarrollada con Spring Boot como prueba técnica para practicante Java.

## Tecnologías y dependencias

| Tecnología     | Versión   | Propósito                           |
|----------------|-----------|-------------------------------------|
| Java           | 21        | Lenguaje base                       |
| Spring Boot    | 3.5.15    | Framework principal                 |
| Spring Data JPA| -         | Persistencia y repositorios         |
| Spring Web     | -         | Controladores REST                  |
| Spring Validation| -       | Validación de DTOs                  |
| H2 Database    | -         | Base de datos en memoria            |
| Lombok         | -         | Reducción de boilerplate            |
| JUnit 5        | -         | Pruebas unitarias                   |
| Mockito        | -         | Mocking en pruebas                  |
| JaCoCo         | 0.8.12    | Cobertura de código                 |

## Arquitectura

Se eligió una arquitectura en capas (`Controller → Service → Repository`) siguiendo principios SOLID:

- **Controller**: Expone endpoints REST, delega lógica al servicio y maneja HTTP.
- **Service**: Contiene las reglas de negocio (cálculo de costos, validaciones de estado). `RentalCostCalculator` se separó de `RentalService` aplicando **Single Responsibility Principle**.
- **Repository**: Capa de persistencia con Spring Data JPA sobre H2 en memoria.
- **Exception**: Manejo global de errores con `@RestControllerAdvice` que devuelve códigos HTTP apropiados y mensajes descriptivos.

**Justificación**: Separar el calculador de costos permite probar las reglas de negocio RN-02 y RN-03 unitariamente sin necesidad de base de datos, cumpliendo con el principio de responsabilidad única.

## Supuestos y decisiones de diseño

### Reglas de negocio
- Los nombres de los tipos de bicicleta usan caracteres ASCII (`MONTANA`, `ELECTRICA`) en lugar de `MONTAÑA` y `ELÉCTRICA` porque Java no permite caracteres con tildes ni eñes en identificadores de enum.
- La duración mínima facturable es **1 hora** (incluso si el cliente devuelve al instante), porque una bicicleta alquilada aunque sea por minutos sigue ocupando un recurso.
- La duración estimada mínima es **1 hora** (`@Min(1)` en `RentalRequest.estimatedHours`).
- La multa se calcula como `(horas facturables - horas estimadas) × (tarifa × 50%)`. Esto es matemáticamente equivalente a lo que pide el enunciado (redondear el retraso al alza) porque `ceil(a) - b = ceil(a - b)` cuando `b` es entero.

### Validaciones
- Si no se envía `startTime` al iniciar un alquiler, se usa `LocalDateTime.now()`.
- Si no se envía `endTime` al finalizar un alquiler, se usa `LocalDateTime.now()`.
- `endTime` no puede ser anterior a `startTime` (se rechaza con 400).
- No se permite registrar dos bicicletas con el mismo código (se rechaza con 400).
- El historial de una bicicleta sin alquileres devuelve una lista vacía (`[]`), no un error.

### Arquitectura
- Se usó `BigDecimal` para valores monetarios en lugar de `double`, evitando errores de redondeo.
- Se optó por H2 en memoria (sin necesidad de instalar base de datos externa).
- Se usaron test slices (`@WebMvcTest`) para los controllers en lugar de arrancar toda la aplicación, haciendo las pruebas más rápidas.
- El `DataInitializer` verifica `count() > 0` antes de insertar, para no duplicar datos si la aplicación se reinicia.
- Formato de respuesta de error: `{"error": "tipo", "message": "descripción"}` para errores simples, y `{"error": "Validation Failed", "details": {"campo": "mensaje"}}` para errores de validación.

## Cómo ejecutar localmente

```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/bike-rental.git
cd bike-rental

# Compilar y ejecutar tests
./mvnw clean test

# Ejecutar la aplicación
./mvnw spring-boot:run
```

La aplicación arranca en `http://localhost:8080`. La consola H2 está disponible en `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:testdb`).

Al iniciar, se precargan 5 bicicletas de datos de referencia:
- BIC-001 (URBANA, DISPONIBLE)
- BIC-002 (MONTANA, DISPONIBLE)
- BIC-003 (ELECTRICA, DISPONIBLE)
- BIC-004 (MONTANA, EN_MANTENIMIENTO)
- BIC-005 (URBANA, DISPONIBLE)

## Endpoints de la API

### Crear bicicleta
```bash
curl -X POST http://localhost:8080/api/bicycles \
  -H "Content-Type: application/json" \
  -d '{"code":"BIC-100","type":"URBANA","status":"DISPONIBLE"}'
```

### Consultar bicicletas disponibles (con filtro opcional por tipo)
```bash
# Todas las disponibles
curl http://localhost:8080/api/bicycles/available

# Filtrar por tipo
curl "http://localhost:8080/api/bicycles/available?type=MONTANA"
```

### Iniciar alquiler
```bash
curl -X POST http://localhost:8080/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"bicycleCode":"BIC-001","customerName":"Juan Pérez","estimatedHours":2,"startTime":"2026-06-14T10:00:00"}'
```

### Finalizar alquiler
```bash
curl -X POST http://localhost:8080/api/rentals/1/finish \
  -H "Content-Type: application/json" \
  -d '{"endTime":"2026-06-14T13:20:00"}'
```

### Consultar historial de una bicicleta
```bash
curl http://localhost:8080/api/rentals/history/BIC-001
```

### Colección de Postman

El archivo `postman/BikeRental.postman_collection.json` contiene **12 requests** preconfiguradas con tests de validación. Para importarla: **Postman → Import → seleccionar el archivo**.

## Pruebas

```bash
./mvnw test
```

```bash
# Con reporte de cobertura
./mvnw clean test
# El reporte HTML se genera en target/site/jacoco/index.html
```

Se incluyen **55 pruebas** que cubren:

**Reglas de negocio (RentalCostCalculatorTest — 22 tests)**
- RN-01/RN-02: Tarifas por tipo y cálculo de costo base (6 parametrizados)
- RN-02: Redondeo al alza de horas facturables (5 tests)
- RN-02/RN-03: Validaciones de entrada (nulls, valores inválidos — 6 tests)
- RN-03: Cálculo de multas por devolución tardía (5 tests, incluido el ejemplo del enunciado)

**Servicio (RentalServiceTest — 17 tests)**
- RF-01/RN-04: Creación, código duplicado, validación de disponibilidad (4 tests)
- RF-02: Inicio de alquiler con cambio de estado (2 tests)
- RF-03/RN-05: Finalización, costos, validaciones de alquiler inexistente/ya terminado (6 tests)
- RF-05: Historial de alquileres (2 tests)

**Controladores REST (BicycleControllerTest + RentalControllerTest — 12 tests)**
- Validación de requests (códigos HTTP 201, 400, 404)
- Filtros inválidos, alquileres inexistentes, casos de error

**Manejo de excepciones (ApiExceptionHandlerTest — 4 tests)**
- NotFound → 404, BadRequest → 400, Validation → 400, error genérico → 500

### Cobertura de código

```
         Instrucciones  Ramas
Global       89%         84%
Controller  100%         75%
Service      91%         87%
Exception   100%         n/a
Enums       100%         n/a
DTOs         72%         n/a
```

El reporte HTML con detalle línea por línea se genera en `target/site/jacoco/index.html`.
