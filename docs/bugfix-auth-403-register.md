# Bug Fix: 403 Forbidden en `POST /api/auth/register`

## Síntoma

Al hacer `POST /api/auth/register` con un body válido, el endpoint devolvía `HTTP 403` con body vacío, sin llegar a ejecutar ninguna lógica de dominio.

---

## Diagnóstico

Con logging `TRACE` de Spring Security se identificaron **dos bugs encadenados**:

```
Secured POST /api/auth/register          ← Spring Security LO DEJÓ PASAR
↓
ObjectOptimisticLockingFailureException  ← FALLÓ en la capa de persistencia
↓
Forward interno a /error                 ← Spring intenta renderizar el error
↓
Access Denied en /error                  ← /error no estaba en permitAll()
↓
HTTP 403                                 ← lo que veía el cliente
```

---

## Bug 1 — `ObjectOptimisticLockingFailureException` al persistir `User`

### Causa raíz

`User.create()` asignaba el `id` con `UUID.randomUUID()` para que los tests unitarios de `JwtService` no explotaran con NPE. El problema: Spring Data JPA detecta que el ID no es `null` y llama a `em.merge()` en lugar de `em.persist()`. `merge()` intenta actualizar una fila existente que no existe → excepción.

### Código con el bug

```java
// User.java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;

public static User create(String email, String passwordHash, String firstName,
                          String lastName, String phone, Role role) {
    var user = new User();
    user.id = UUID.randomUUID(); // ← Spring Data JPA ve id != null → merge() → falla
    user.email = email;
    // ...
    return user;
}
```

### Corrección

`User` implementa `Persistable<UUID>`. Un flag `@Transient isNew` se setea a `true` solo en `create()` y vuelve a `false` con `@PostLoad` cuando JPA carga la entidad desde la DB. Así Spring Data siempre llama a `persist()` para entidades nuevas, sin importar si el ID está seteado.

Se eliminó también `@GeneratedValue` porque ahora el ID lo asigna el dominio.

```java
// User.java — corregido
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User implements Persistable<UUID> {

    @Id
    private UUID id; // sin @GeneratedValue — el dominio asigna el ID

    // ... resto de campos ...

    @Transient
    private boolean isNew = false;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    void markNotNew() {
        this.isNew = false; // JPA setea esto al cargar desde DB
    }

    public static User create(String email, String passwordHash, String firstName,
                              String lastName, String phone, Role role) {
        var user = new User();
        user.id = UUID.randomUUID();
        user.isNew = true; // ← le dice a Spring Data que es nueva → persist()
        user.email = email;
        // ...
        return user;
    }
}
```

---

## Bug 2 — `/error` bloqueado por Spring Security

### Causa raíz

Cuando ocurre una excepción no manejada en el controller, Spring hace un **forward interno** a `/error` para renderizar la respuesta de error. Este path no estaba en la lista de `permitAll()`, por lo que el `AuthorizationFilter` lo bloqueaba con 403 en lugar de dejar pasar la respuesta de error original.

### Código con el bug

```java
// SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**", "/actuator/health").permitAll() // ← falta /error
    .anyRequest().authenticated()
)
```

### Corrección

```java
// SecurityConfig.java — corregido
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**", "/actuator/health", "/error").permitAll()
    .anyRequest().authenticated()
)
```

---

## Por qué los tests no detectaron esto

Los tests de `AuthControllerTest` usan `@WebMvcTest` con `SecurityAutoConfiguration` excluida:

```java
@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
    }
)
```

Al excluir toda la capa de seguridad y mockear los puertos de persistencia, ningún test pasaba por `JwtAuthFilter`, `AuthorizationFilter` ni por la DB real — por eso ambos bugs pasaron desapercibidos.

---

## Resultado

```
POST /api/auth/register  →  HTTP 201
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "userId": "1aa9c5b4-...",
  "email": "seller@test.com",
  "role": "SELLER"
}
```

Todos los tests del módulo de auth continúan en verde: **21/21**.
