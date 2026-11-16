# Lab 7 - how to put this into your project

This archive contains the complete `src/` folder and `pom.xml` of spring-lab-01
= your lab 6 work (branch `lab06`) + everything for lab 7.

NOTE: your `main` branch does not contain lab 6 (it was never merged), and your
local `lab07` branch was created from `main`. Lab 7 builds on lab 6 (Genre,
validation, exceptions), so start lab07 from `lab06`.

```bash
cd spring-lab-01
git stash -u                  # park the half-finished files (nothing is lost)
git switch -C lab07 lab06     # lab07 now starts from your lab 6 work
```

Then:
1. Delete the project's `src/` folder and its `pom.xml`.
2. Copy `src/` and `pom.xml` from this archive into the project root.
3. In IntelliJ: right-click pom.xml -> Maven -> Reload project.
4. Run `SpringLab01Application` (JDK 25).

Quick check:  http://localhost:8080/api/books   and   http://localhost:8080/swagger-ui.html

Commit when everything works:
```bash
git add .
git commit -m "lab07: REST API, DTOs, problem details, OpenAPI"
git push -u origin lab07
```

## What changed compared with lab 6
NEW      web/BookApiController.java      five operations, OpenAPI annotations
NEW      web/dto/BookRequest.java        request record (validation from lab 6)
NEW      web/dto/BookResponse.java       response record
NEW      web/dto/BookMapper.java         toDomain / toResponse / toResponseList
NEW      catalog/DuplicateTitleException.java   -> 409
NEW      src/test/http/books.http        13 ready requests for the IDEA HTTP client
CHANGED  web/ApiExceptionHandler.java    now extends ResponseEntityExceptionHandler, adds 409 and field errors on 400
CHANGED  web/PageExceptionHandler.java   scoped with assignableTypes (see comment in the file)
CHANGED  catalog/Book.java               + genre, + createdAt
CHANGED  catalog/BookRepository.java     new fields, stable ordering by id
CHANGED  catalog/BookService.java        409 on duplicates, 404 on update/delete of a missing id
MOVED    web/Genre.java -> catalog/Genre.java   (so that catalog does not depend on web)
CHANGED  web/BookForm.java, web/BookPageController.java   adapted to the new Book record
CHANGED  pom.xml                         + springdoc-openapi-starter-webmvc-ui 3.1.1
REMOVED  web/BookRestController.java     replaced by BookApiController (same URL /api/books)
