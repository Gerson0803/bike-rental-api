# Guía de Explicación para la Entrevista (≈25 min)

---

## 1. Contexto y problema (2 min)

"Una empresa de turismo urbano alquila bicicletas en varios puntos de la ciudad. Hasta ahora usaban hojas de cálculo y tenían tres problemas graves:

1. **Cobros incorrectos** — no aplicaban bien las tarifas por tipo de bicicleta
2. **Disponibilidad inconsistente** — bicicletas marcadas como disponibles cuando ya estaban alquiladas
3. **Multas no calculadas** — no penalizaban las devoluciones tardías

Me pidieron construir una API REST que resolviera esto, con Spring Boot, pruebas automatizadas y reglas de negocio bien definidas."

---

## 2. Tecnologías y por qué las elegí (2 min)

| Tecnología | Por qué |
|------------|---------|
| **Spring Boot 3.5** | Framework estándar para APIs REST en Java |
| **Java 21** | Última LTS, records, pattern matching |
| **H2 en memoria** | No requiere instalación, ideal para desarrollo y pruebas |
| **JPA/Hibernate** | Persistencia simple con entidades y repositorios |
| **JUnit 5 + Mockito** | Tests unitarios con mocking |
| **JaCoCo** | Medir cobertura de código |
| **GitHub Actions** | CI/CD integrado con el repositorio |
| **AWS EC2** | Despliegue en cloud gratuito (t3.micro) |

"Elegí H2 por simplicidad — quien revise el proyecto solo necesita `mvnw spring-boot:run` y ya tiene todo. En producción se cambiaría por PostgreSQL sin tocar una línea de código, solo cambiando la URL en application.properties."

---

## 3. Arquitectura (3 min)

"Mantuve una arquitectura en capas tradicional:

```
Controller → Service → Repository (JPA)
                ↕
         RentalCostCalculator
```

**Controller** — Recibe HTTP, valida entrada, delega al servicio, devuelve JSON.

**Service** — Orquesta las operaciones, aplica @Transactional para que cada operación sea atómica.

**RentalCostCalculator** — Clase separada del servicio (Single Responsibility Principle). Contiene todas las reglas de cálculo: tarifas, redondeo, multas. Esto permite testear las reglas sin necesidad de base de datos ni mockear repositorios.

**Repository** — Spring Data JPA genera las consultas automáticamente.

**Exception Handler** — Clase global con @RestControllerAdvice que captura cualquier error y devuelve el HTTP code apropiado (400, 404, 500) con mensajes descriptivos."

"La separación del Calculator del Service fue intencional — si mañana cambian las tarifas, solo tocas esa clase. Si cambia la lógica de persistencia, solo tocas el Service."

---

## 4. Endpoints de la API (3 min)

"Mostrá los endpoints uno por uno:"

```bash
# 1. Crear bicicleta
POST /api/bicycles
Body: {"code":"BIC-100","type":"URBANA","status":"DISPONIBLE"}
→ 201 Created

# 2. Listar disponibles (con filtro opcional por tipo)
GET /api/bicycles/available
GET /api/bicycles/available?type=MONTANA
→ 200 OK

# 3. Iniciar alquiler
POST /api/rentals
Body: {"bicycleCode":"BIC-001","customerName":"Juan","estimatedHours":2}
→ 201 Created, bicicleta pasa a ALQUILADA

# 4. Finalizar alquiler
POST /api/rentals/{id}/finish
Body: {"endTime":"2026-06-14T13:20:00"}
→ 200 OK, calcula costo + multa, bicicleta vuelve a DISPONIBLE

# 5. Historial
GET /api/rentals/history/BIC-001
→ 200 OK, lista de alquileres con costos
```

"Todos los endpoints devuelven JSON, todos tienen validación de entrada, y todos manejan errores con HTTP codes adecuados."

---

## 5. Reglas de negocio en detalle (5 min)

"Esta es la parte que el entrevistador probablemente más va a preguntar."

### RN-01: Tarifas

"Las tarifas están definidas en el enum `BicycleType` con su costo por hora. Usé `BigDecimal` para evitar errores de redondeo con double:"

```java
URBANA(3500), MONTANA(5000), ELECTRICA(7500)
```

### RN-02: Cálculo de horas

"Las horas se redondean SIEMPRE al alza. 1h10min → 2 horas, 30min → 1 hora."

```java
long minutos = Duration.between(start, end).toMinutes();
long horasFacturables = (long) Math.ceil(minutos / 60.0);
return Math.max(1, horasFacturables);
```

"El `Math.max(1, ...)` aplica mínimo 1 hora. Esto fue un supuesto que documenté en el README."

### RN-03: Multa

"La multa es 50% de la tarifa por cada hora de retraso. Acá hay un detalle matemático interesante:"

"El enunciado dice: calcular retraso en horas, redondear al alza, multiplicar por 50% de la tarifa. Yo en el código uso directamente `(horas facturables - horas estimadas) × tarifa × 50%`. Esto funciona porque matemáticamente `ceil(a) - b = ceil(a - b)` cuando b es entero."

"Ejemplo del enunciado: Montaña, estimada 2h, devuelta a las 3h20min:
- Horas facturables: 4h
- Costo base: 4 × $5.000 = $20.000
- Retraso: 4 - 2 = 2h
- Multa: 2 × ($5.000 × 50%) = $5.000
- Total: $25.000"

### RN-04: No alquilar si no está disponible

"Antes de iniciar un alquiler, valido que `bicycle.status == DISPONIBLE`. Si es ALQUILADA o EN_MANTENIMIENTO, rechazo con 400."

### RN-05: No finalizar alquiler inexistente o ya terminado

"Verifico que el rental exista (si no → 404). Verifico que `endTime == null` en la BD (si ya tiene endTime → ya terminó → 400)."

---

## 6. Errores que encontré y corregí (2 min)

"Cuando recibí el código base, tenía **3 errores de compilación** que impedían ejecutarlo:"

1. **Import faltante:** `RentalService.java` usaba `BicycleType.valueOf()` pero no importaba la clase `BicycleType`.
2. **Método no declarado:** `RentalService.getRentalHistory()` llamaba a `rentalRepository.findByBicycle()` pero ese método no existía en la interfaz.
3. **Firma incorrecta:** `ApiExceptionHandler` usaba `@Override` con `HttpStatus` pero el método padre esperaba `HttpStatusCode`. Esto hacía que no se ejecutara el manejador de validación.

"Además, no había datos de inicialización (las 5 bicicletas del enunciado no se cargaban al arrancar), solo había 1 test que no probaba nada, y no había README."

---

## 7. Pruebas (4 min)

"Escribí **55 tests** organizados así:"

### RentalCostCalculatorTest (22 tests)
- "Validación de horas facturables: 1h10min → 2h, 30min → 1h, start null → error, end null → error"
- "Cálculo de costo base: probé todas las combinaciones de tipo y horas con @CsvSource"
- "Multas: probé el ejemplo exacto del enunciado ($25.000), devolución temprana (multa $0), atraso con cada tipo de bicicleta"
- "Casos borde: type null → error, hours 0 → error"

### RentalServiceTest (17 tests)
- "Creación exitosa y con código duplicado"
- "Alquiler de bicicleta disponible → pasa a ALQUILADA"
- "Alquiler de bicicleta no disponible (ALQUILADA o EN_MANTENIMIENTO) → 400"
- "Finalización exitosa → bicicleta vuelve a DISPONIBLE, calcula costos correctamente"
- "Finalización de alquiler inexistente → 404"
- "Finalización de alquiler ya terminado → 400"
- "Fin sin endTime → usa hora actual"
- "Historial de bicicleta existente e inexistente"

### Controller tests (12 tests)
"Usé @WebMvcTest con MockMvc para probar cada endpoint HTTP:"
- "201 cuando los datos son válidos"
- "400 cuando faltan campos (código vacío, tipo null, estimatedHours=0)"
- "400 con filtro de tipo inválido"
- "404 cuando el recurso no existe"

### Exception handler (4 tests)
- "Probé que NotFoundException → 404 con formato correcto"
- "BadRequestException → 400"
- "Validation errors → 400 con detalles campo por campo"
- "Error genérico → 500"

### Cobertura
"JaCoCo reporta **89% de instrucciones** y **84% de ramas**. Lo que queda sin cubrir son getters/setters de DTOs (72%) y la rama del DataInitializer que evita inserts duplicados."

---

## 8. CI/CD con GitHub Actions (2 min)

"Configuré dos workflows:"

**CI** (`.github/workflows/ci.yml`):
- Se ejecuta en cada push
- Build con Maven
- Ejecuta los 55 tests
- Empaqueta el JAR
- Sube el artefacto

**Deploy** (`.github/workflows/deploy.yml`):
- Se ejecuta automáticamente cuando el CI pasa
- Descarga el JAR
- Lo copia por SSH a EC2
- Mata el proceso anterior
- Inicia la nueva versión con nohup

"Uso un archivo `app.pid` para trackear el proceso y poder matarlo limpiamente en el próximo deploy."

---

## 9. Despliegue en AWS (1 min)

"La API está corriendo en una instancia **EC2 t3.micro** (free tier) con Ubuntu 24.04 y Java 21."

"Security group con puertos: 22 (SSH para deploy) y 8080 (HTTP para la API)."

"URL pública: http://13.220.253.30:8080 — funciona, pueden probarla ahora mismo."

---

## 10. Decisiones de diseño adicionales (1 min)

"Algunas decisiones que tomé y documenté como supuestos en el README:"

1. **Mínimo 1 hora facturable** — aunque devuelvan al instante, porque el recurso estuvo ocupado
2. **BigDecimal para montos** — nunca double para dinero
3. **ASCII en enums** — `MONTANA` en vez de `MONTAÑA` porque Java no permite ñ en identificadores
4. **H2 en memoria** — cero configuración para quien revise el proyecto
5. **Constructor injection** — mejor que @Autowired para testear y seguir principios SOLID
6. **@WebMvcTest en controllers** — más rápidos que @SpringBootTest porque no arrancan toda la aplicación

---

## 11. Preguntas que podrían hacerte (lista de chequeo)

| Pregunta | Respuesta |
|----------|-----------|
| ¿Por qué no usaste MySQL/PostgreSQL? | H2 es perfecto para desarrollo y CI; en prod se cambia la URL en application.properties |
| ¿Cómo escalaría esto? | Agregando un load balancer y más instancias EC2 detrás |
| ¿Por qué BigDecimal y no double? | double tiene errores de redondeo en cálculos financieros |
| ¿Qué harías con más tiempo? | Agregaría autenticación JWT, paginación en historial, y test de integración con base de datos real |
| ¿Por qué separaste RentalCostCalculator? | SRP — si cambian tarifas no tocas el servicio; y permite testear sin BD |
| ¿Cómo manejas concurrencia? | @Transactional con isolation DEFAULT en Spring + JPA; si dos personas alquilan la misma bici, el segundo recibe error porque el estado ya cambió |
