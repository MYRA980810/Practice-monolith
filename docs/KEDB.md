# Known Error Database (KEDB) — LiveComerce

---

## KEDB-001 — 403 Forbidden en endpoint protegido con JWT válido

| Campo        | Detalle                                                   |
|--------------|-----------------------------------------------------------|
| **ID**       | KEDB-001                                                  |
| **Módulo**   | `auth` / `store`                                          |
| **Endpoint** | `POST /api/stores`                                        |
| **Fecha**    | 2026-04-22                                                |
| **Estado**   | Resuelto                                                  |

### Síntomas
- Postman retorna `403 Forbidden` al llamar a un endpoint con `@PreAuthorize`.
- No se muestra ningún log en la consola de la aplicación.
- El token fue obtenido correctamente del endpoint `/api/auth/login` o `/api/auth/register`.

### Causa Raíz
El token JWT fue copiado desde el cuerpo JSON de la respuesta **incluyendo las comillas** del formato JSON:

```json
{ "accessToken": "eyJhbGci...fvj" }
```

Se copió `"eyJhbGci...fvj"` en lugar de `eyJhbGci...fvj`. La comilla `"` al inicio y al final se envía como parte del token en el header `Authorization`, lo que hace que la firma no coincida y la autenticación falle silenciosamente.

### Diagnóstico
El `JwtAuthFilter` tenía un `catch (Exception ignored)` que tragaba la excepción sin loguearla, ocultando el error real. El problema se identificó agregando `log.warn` en el catch y `log.info` en `JwtService.generate()` y `JwtService.validateAndExtract()` para comparar el token generado con el recibido.

Señal clave en los logs:
```
Generated:  ...1zVfxsFJ4eXWZemY8fvj
Validating: ...zVfxsFJ4eXWZemY8fvj"   ← comilla extra al final
```

### Resolución
En Postman → pestaña **Authorization** → tipo **Bearer Token** → pegar **únicamente** el valor del campo `accessToken` sin comillas.

### Correcciones aplicadas al código
- `JwtAuthFilter`: reemplazado `catch (Exception ignored)` por `log.warn("JWT validation failed: {}", e.getMessage())`.
- `JwtAuthFilter`: agregado `log.info` post-autenticación para registrar el usuario y sus authorities.
- `JwtService`: agregado logging temporal en `generate()` y `validateAndExtract()` para diagnóstico.

---

## KEDB-002 — `@PreAuthorize` no tiene efecto (silenciosamente ignorado)

| Campo        | Detalle                                                   |
|--------------|-----------------------------------------------------------|
| **ID**       | KEDB-002                                                  |
| **Módulo**   | `auth` / seguridad                                        |
| **Fecha**    | 2026-04-22                                                |
| **Estado**   | Resuelto                                                  |

### Síntomas
- Los endpoints anotados con `@PreAuthorize("hasRole('SELLER')")` no restringen el acceso por rol.
- Cualquier usuario autenticado puede acceder sin importar su rol.

### Causa Raíz
`@EnableMethodSecurity` no estaba declarado en `SecurityConfig`. Sin esta anotación, Spring Security ignora completamente todas las anotaciones `@PreAuthorize`, `@PostAuthorize` y `@Secured`.

### Resolución
Agregar `@EnableMethodSecurity` en `SecurityConfig`:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // ← requerido para que @PreAuthorize funcione
@EnableConfigurationProperties(JwtProperties.class)
class SecurityConfig { ... }
```

---

## KEDB-003 — Spring Boot no levanta: error de I/O en PostgreSQL

| Campo        | Detalle                                                         |
|--------------|-----------------------------------------------------------------|
| **ID**       | KEDB-003                                                        |
| **Módulo**   | Infraestructura / Base de datos                                 |
| **Fecha**    | 2026-04-22                                                      |
| **Estado**   | Resuelto                                                        |

### Síntomas
```
SQL State  : 58030
Error Code : 0
Message    : FATAL: could not open file "global/pg_filenode.map": I/O error
```
La aplicación Spring Boot no levanta y falla al intentar conectarse a la base de datos.

### Causa Raíz
El contenedor Docker de PostgreSQL fue detenido de forma abrupta (o el volumen fue desmontado sin cerrar correctamente el proceso), corrompiendo o dejando inaccesible el archivo `global/pg_filenode.map` del data directory de Postgres.

### Resolución
Reiniciar el contenedor Docker:

```bash
docker compose down && docker compose up -d
```

Verificar que el contenedor esté `healthy` antes de iniciar Spring:

```bash
docker compose ps
```

Si el error persiste después del reinicio, revisar los logs del contenedor:

```bash
docker compose logs postgres
```

---

## KEDB-004 — 403 sin distinción de 401 (usuario no autenticado vs. no autorizado)

| Campo        | Detalle                                       |
|--------------|-----------------------------------------------|
| **ID**       | KEDB-004                                      |
| **Módulo**   | `auth` / seguridad                            |
| **Fecha**    | 2026-04-22                                    |
| **Estado**   | Pendiente de mejora                           |

### Síntomas
- Requests sin token o con token inválido retornan `403` en lugar de `401`.
- Es imposible distinguir desde el cliente si el error es de autenticación o de autorización.

### Causa Raíz
`SecurityConfig` no tiene un `AuthenticationEntryPoint` configurado. Por defecto, Spring Security retorna `403` para ambos casos: usuario no autenticado y usuario sin permisos suficientes.

### Resolución Recomendada
Agregar manejo de excepciones en `SecurityConfig`:

```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint((req, res, e) -> res.sendError(401, "Unauthorized"))
)
```

---

*Documento generado a partir de la sesión de debugging del 2026-04-22.*
