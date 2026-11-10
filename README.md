# web-security26
Java 25, Spring Boot 4.1, MongoDB (Docker)

## Run
1. `docker compose up -d`
2. Open the folder in IntelliJ IDEA, wait for Maven import, run `WebSecurity26Application`
   (or `mvn spring-boot:run`)
3. Open a file from `http/` and click Run All Requests in File.

## Labs
- Lab 1: Item CRUD at /api/v1/items
- Lab 2: http/lab2-crud.http
- Lab 3: app.csrf.enabled=false, http/lab3-basic-authorization.http (admin/admin)
- Lab 4: app.csrf.enabled=true, http/lab4-csrf.http + auditing (createdBy, createdDate, ...)
- Lab 5: 3 users (admin, manager, user), role-based access, http/lab5-roles.http
- Lab 6: method security annotations (`@PreAuthorize`, `@Secured`, `@RolesAllowed`) in
  `AnnotatedController` (/api/v1/annotated/**), http/lab6-annotations.http (10 requests, 3 users)
- Lab 7: JUnit access tests `src/test/java/.../AccessTest.java` (+ `JwtTest.java`), run `mvn test`
  or right click `src/test/java` -> Run 'All Tests' (MongoDB is not needed for tests)
- Lab 8: JWT login `POST /api/v1/auth/authenticate` `{"email":"user@mail.com","password":"user"}`
  -> `{"token":"..."}` (HS384). http/lab8-jwt-login.http, check the token at https://jwt.io
  (secret: value of `app.jwt.secret`)
- Lab 9: access with `Authorization: Bearer <token>`: http/lab9-jwt-access.http,
  Postman/Talend collection `postman/web-security26-jwt.postman_collection.json`

## Users
| username | email | password | role |
|----------|-------|----------|------|
| admin | admin@mail.com | admin | ADMIN |
| manager | manager@mail.com | manager | MANAGER |
| user | user@mail.com | user | USER |
