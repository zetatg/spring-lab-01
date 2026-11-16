# spring-lab-01 — RWPSF 3305, laboratory works 1–11

Turmakov Akylbek, IS-2407. One Spring Boot 4.1 / Java 25 project that grows lab by lab.

## Run it (no Docker needed)
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```
- **dev** profile (lab 3): port **8081**, demo books seeded (lab 8), DEBUG logging → http://localhost:8081
- no profile: port 8080, empty catalogue · **prod**: port 8080
- IntelliJ: Run → Edit Configurations → *Active profiles*: `dev`
- Logins (lab 10): `user / user123`, `admin / admin123`
- Database: H2 file `./data/springlab.mv.db` (delete the `data` folder to start clean).
  H2 console: log in as admin → `/h2-console`, JDBC URL `jdbc:h2:file:./data/springlab;NON_KEYWORDS=YEAR,VALUE;AUTO_SERVER=TRUE`, user `sa`, empty password.
- PostgreSQL instead (lab 8 setup): `docker run -d --name pg-springlab -e POSTGRES_DB=springlab -e POSTGRES_USER=springlab -e POSTGRES_PASSWORD=springlab -p 5432:5432 postgres:16`, then profiles `dev,postgres`.

## Tests
```powershell
.\mvnw.cmd test        # 56 tests: Labs1to8Tests, Lab7HateoasTests, TransactionLabTests, SecurityTests, Lab11Tests
```

## What each lab added
| Lab | Topic | Where | Try it (dev, port 8081) | Individual variant |
|---|---|---|---|---|
| 1 | First Spring Boot app | `web/HelloController` | `/api/hello?name=IITU`, `/api/info` | **8** — `/api/case?text=hello world` |
| 2 | IoC, DI, scopes, lifecycle | `notify/*`, `lifecycle`, `scope`, `config/FormatConfig`, `config/ContainerReport`, `web/Lab2Controller` | `/api/lab2/notify?text=IITU`, `/scopes`, `/lifecycle` | **8** — `html` notifier, `/api/lab2/custom?text=<b>hi</b>` |
| 3 | YAML, profiles, typed config, Actuator | `application*.yml`, `config/AppProperties`, `config/EnvironmentBanner`, `web/Lab3Controller` | `/api/lab3/config`, `/actuator/health` (env/configprops/conditions: admin) | **8** — `app.export` (formats, max-rows, include-header) |
| 4 | Spring AOP | `aspect/*`, `audit/Audited`, `service/CatalogService`, `controller/CatalogController` | `/api/lab4/items`, `/proxy`, `/remove-twice-this/5` (before fix) vs `/remove-twice/5` (after) | **7** — `@RateLimit(perMinute = 5)` + `RateLimitAspect`: call `/api/lab4/item/1` six times → 429 + `Retry-After`. (Variant 1 from the pair report also kept: `/api/lab4/stats`) |
| 5 | REST + MVC controllers | `web/BookApiController`, `web/BookPageController`, `static/css/app.css` | `/books`, `/api/books` | **7** — `POST /api/books/bulk` (JSON array → 201 + list; all or nothing). Extra: `/api/books/search?title=` |
| 6 | Forms, validation, i18n | `web/BookForm`, `web/NoDigits`, `templates/books/form.html`, `messages*.properties` (EN/RU/KZ) | `/books/new?lang=ru` | **7** — publication date: `LocalDate`, `@PastOrPresent`, `@DateTimeFormat(style="S-")` → EN `1/6/18`, RU `06.01.2018`, KZ `06.01.18`. Extras: ISBN `@Isbn`, Kazakh (variant 9) |
| 7 | REST API: DTOs, problem details, OpenAPI | `web/dto/*`, `web/ApiExceptionHandler` | `/api/books/999`, `/swagger-ui.html` | **7** — HATEOAS: `/api/hal/books/1` → `_links`: self, books, author-books (`BookHateoasController`). Extra: paging metadata |
| 8 | JPA: entities, associations, queries, paging | `catalog/*`, `config/DataSeeder`, `config/Lab8Demo` (profile `demo`) | `/api/books?genre=PROGRAMMING&sort=year,desc` | **7** — keyset scrolling: `/api/books/scroll`, `/api/books/scroll?after=3` (`Window<Book>`, SQL `where id>? order by id fetch first ? rows`, no `offset`). Extra: `Publisher` entity |
| 9 | Transactions, Flyway | `tx/*`, `db/migration/V1–V4`, `TransactionLabTests`; separate module `lab9-transaction-demo` | H2 console: `select * from flyway_schema_history` | — |
| 10 | Spring Security | `security/*`, `V5__security.sql`, `V6__remember_me.sql`, `templates/login.html`, `error/403.html`, `admin/users.html` | `/login`, `/admin` | **7** — remember-me: checkbox on `/login`, cookie `remember-me` for 14 days, tokens in `persistent_logins`. Extras: variants 1, 3, 12 |
| 11 | JWT, OAuth/OIDC, RBAC, ABAC | `lab11/*`, `Lab11Tests` | `/api/lab11/sample`, `/api/lab11/pkce` | — |

Variants (register no. **19**): labs 1–3 use "number mod 12 + 1" = **8**; labs 4–8 and 10 use the position in the group list, 19 → wraps past 12 → **7**. Earlier variant-1 work is kept as extras.

Labs 1–4 GET endpoints are public; `/api/books` and `/books` need a login (lab 10). Writes need `admin` **and** a CSRF token.

## Lab 3 — profiles and source precedence
```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target\spring-lab-01-0.0.1-SNAPSHOT.jar                                  # port 8080, banner "no profile is active"
java -jar target\spring-lab-01-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev     # port 8081, retryCount 1
$env:APP_MAIL_RETRYCOUNT = "7"; java -jar target\spring-lab-01-0.0.1-SNAPSHOT.jar  # environment variable
java -Dapp.mail.retry-count=5 -jar target\spring-lab-01-0.0.1-SNAPSHOT.jar         # system property
java -jar target\spring-lab-01-0.0.1-SNAPSHOT.jar --app.mail.retry-count=9         # command line wins
```
Experiment: set `app.mail.retry-count: 99` in `application.yml` → start-up fails with the violated `@Max(10)`.

## Lab 10 variant 7 — remember-me
Log in with *Remember me for 14 days* ticked, then open DevTools → Application → Cookies: `remember-me`, Expires = +14 days, HttpOnly.
Close the browser, open it again: you are still logged in. In the H2 console: `select * from persistent_logins` — `token` changes on every automatic login; logging out deletes the row.

## Lab 10 — CSRF demonstration (PowerShell, `curl.exe`)
```powershell
# 1. POST with credentials but WITHOUT the token -> 403
curl.exe -i -u admin:admin123 -X POST localhost:8081/books -d "title=Forged&author=Nobody&genre=FICTION&year=2020"
# 2. open the form page with a session and read the token
curl.exe -s -c cookies.txt -u admin:admin123 localhost:8081/books/new | Select-String '_csrf'
# 3. the same POST WITH the session and the token -> 302, Location: /books
curl.exe -i -b cookies.txt -u admin:admin123 -X POST localhost:8081/books -d "title=Genuine&author=Somebody&genre=FICTION&year=2020&_csrf=<paste the token>"
```

## Lab 11 — tokens (all synthetic)
```powershell
$t = (curl.exe -s localhost:8081/api/lab11/tokens/VALID | ConvertFrom-Json).token
curl.exe -s -H "Authorization: Bearer $t" localhost:8081/api/lab11/validate
curl.exe -i -H "Authorization: Bearer $t" localhost:8081/api/lab11/invoices/42      # 200
curl.exe -i -H "Authorization: Bearer $t" localhost:8081/api/lab11/invoices/43      # 403 confidential
curl.exe -s "localhost:8081/api/lab11/rbac?role=viewer&action=invoices:update"
curl.exe -s localhost:8081/api/lab11/audit
```
Token kinds: VALID, EXPIRED, WRONG_AUDIENCE, WRONG_ISSUER, ID_TOKEN, FORGED_SIGNATURE, UNSIGNED, NO_SCOPE, OTHER_DEPARTMENT.
