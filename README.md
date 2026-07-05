# ecommerce-api

A small e-commerce REST API built with Kotlin and Spring Boot, backed by MySQL and
Elasticsearch, with catalog data ingested from [dummyjson.com/products](https://dummyjson.com/products).

MySQL is the source of truth. Elasticsearch is a derived, read-optimized search index
built by reading the catalog back out of MySQL — the two stores can never disagree,
because Elasticsearch is never populated from anywhere else.

## How to run

Prerequisites: Docker and Docker Compose. No local JDK or Gradle install needed — the
app is built inside the Docker image.

```
git clone git@github.com:a4ary4n/ecommerce-api.git
cd ecommerce-api
docker compose up
```

That's it. On first run this builds the app image, starts MySQL and Elasticsearch,
waits for both to report healthy, then boots the app — which automatically loads the
full catalog (194 products from dummyjson) into MySQL and indexes it into
Elasticsearch before it's done starting. A few seconds after the containers report
healthy, the API is live at `http://localhost:8080`:

```
curl http://localhost:8080/categories
curl http://localhost:8080/products?query=phone
curl http://localhost:8080/products/1
```

Re-running `docker compose up` (or restarting the `app` container) re-loads the
catalog from scratch every time — this is intentional (see [Ingestion
idempotency](#ingestion-pipeline) below), not a bug, and safe to do repeatedly.

`docker compose down` stops everything and keeps the data volumes; `docker compose
down -v` also wipes them for a genuinely clean slate.

**Local development** (what I used day to day): run just the two data stores —
`docker compose up -d mysql elasticsearch` — and run the app itself from IntelliJ
against `localhost` ports. In this mode ingestion does *not* run automatically
(`app.ingest.mysql.enabled` / `app.ingest.elasticsearch.enabled` default to `false`
outside the compose `app` service); trigger it manually when you want fresh data,
e.g. `./gradlew bootRun --args='--app.ingest.mysql.enabled=true --app.ingest.elasticsearch.enabled=true'`.

## Architecture overview

```
dummyjson.com  --(sync)-->  MySQL  --(reindex)-->  Elasticsearch
                                |                        |
                                +------ REST API --------+
                          /categories, /products/{id}   /products (search)
```

Ingestion is two independently triggerable steps, not one combined pipeline:

1. **Sync** (dummyjson → MySQL) — fetches the full catalog and categories from
   dummyjson, wipes and reloads all 7 MySQL tables.
2. **Reindex** (MySQL → Elasticsearch) — reads the catalog straight back out of
   MySQL and rebuilds the `products` Elasticsearch index from it. This step never
   talks to dummyjson at all; its only dependency is MySQL.

Both are gated behind their own property flag and run as `CommandLineRunner`s on
app startup, ordered so sync always finishes before reindex when both are enabled
together (as they are in the Docker deployment).

The REST layer mirrors that same split: `GET /products` (list/search/filter) reads
from the Elasticsearch index; `GET /products/{id}` reads full detail straight from
MySQL; `GET /categories` reads from MySQL.

Package layout, roughly by responsibility:

```
entity/       JPA entities — the 7-table schema
repository/   Spring Data JPA repositories
search/       Elasticsearch document, repository, and query builder
ingestion/    the two-flow pipeline above (dummyjson client, mappers, writer/reader/indexer)
service/      REST-facing business logic (backs the controllers)
web/          controllers and request/response DTOs
config/       the RestClient bean used to call dummyjson
```

## Design decisions

### Data model (MySQL)

- **`products.id` reuses dummyjson's own product id** rather than generating a new
  one — it's already a stable natural key, and remapping it would add an indirection
  layer for no benefit.
- **Money and ratings are `DECIMAL` in MySQL, `double` in Elasticsearch** —
  deliberately asymmetric. MySQL needs exactness; Elasticsearch is a derived index
  where float semantics are the norm and don't affect correctness anywhere that matters.
- **`rating` is stored as given by the source, never recomputed from the `reviews`
  table** — the reviews dummyjson embeds are a partial sample, not the full
  population the original rating was computed from, so recomputing it would produce
  a different (wrong) number.
- **`availability_status` is kept as its own column alongside `stock`** — it encodes
  business logic from the source system (thresholds, possible manual overrides) that
  isn't reliably derivable from the raw stock count alone.
- **Enum values are normalized to machine codes** (`IN_STOCK`, not `"In Stock"`) —
  the stored representation is decoupled from the source's display strings;
  ingestion maps between the two explicitly.
- **Categories, brands, and tags are normalized into their own tables**; 1:1 product
  attributes (dimensions, warranty text, etc.) stay as plain columns. Tags are a
  textbook many-to-many; normalizing single-valued attributes too would be
  over-normalization for no benefit.
- **`categories.url` was dropped** — it's derivable from the slug, and storing it
  separately is redundancy that can silently go stale.
- **URL-typed columns are consistently `VARCHAR(500)`** across all tables, rather
  than picking a different length per column.

### Search index (Elasticsearch)

- **Documents are flat and denormalized**, containing only search-relevant fields
  (id, title, description, category slug, brand, tags, price, discount, rating,
  stock, availability, thumbnail, created date). Reviews, images, dimensions, and
  other detail-only fields are deliberately excluded — that data lives in MySQL and
  is served via `GET /products/{id}`.
- **Documents are built by reading products back out of MySQL after ingestion**,
  never by a parallel fetch from dummyjson. This is what actually guarantees MySQL
  and Elasticsearch can't disagree — not just a nice property, a structural one.
- **Full-text search weights fields by relevance signal strength**: `title^4`,
  `brand^3`, `tags^2`, `description^1`. Title is the most information-dense field; a
  brand match is high-intent, nearly as strong as a title match; tags are loose
  thematic labels; description is prose, the weakest signal.
- **`category` is excluded from full-text search** and is a filter-only field —
  including it in the scored query would drown ranking (e.g. "fragrance" would
  match every product in that category via this field alone, regardless of title
  relevance).
- **`brand` and `tags` are multi-fields** (analyzed text *and* an exact `.keyword`
  sub-field) — searched as prose in the full-text query, filtered exactly when used
  as a structured filter. `category` is keyword-only since it's never meant to be
  searched as prose.
- **Structured filters (category, brand, price range, rating, availability) live in
  Elasticsearch's filter context, not query context** — no scoring cost, cacheable.
  Query and filters compose into a single `bool` query; one query builder handles
  every parameter combination, including all of them at once.
- **Sorting**: an explicit `sort` param always overrides relevance, regardless of
  whether a text query is present. Without one, the default is relevance when
  `query` is present, or alphabetical by title otherwise — a bare listing has no
  natural relevance order to fall back on.
- **`fuzziness: AUTO` on the full-text query** gives cheap typo tolerance. Its
  behavior is worth understanding precisely: it scales the allowed edit distance by
  the query term's length (0 edits for 1–2 characters, 1 edit for 3–5, 2 edits for
  6+). This means very short queries won't fuzzy-match long words — `?query=perf`
  won't match a tag like "perfumes" (needs 4 edits), but `?query=perfum` will (needs
  2, within the 6+-character bucket's allowance). This is typo tolerance on
  complete-ish words, not prefix/incomplete-word autocomplete — a different feature,
  out of scope here (see Known limitations).
- **Pagination is implemented** (`page`/`size`) even though the brief doesn't
  mention it — a listing endpoint that returns the entire catalog in one response
  isn't a realistic design, so I built it as a reasonable assumption rather than
  leaving it unaddressed.

### Ingestion pipeline

- **Idempotency strategy: wipe-and-reload.** Every sync run deletes all 7 tables (in
  FK-safe order) and reloads fresh from dummyjson, rather than attempting an
  incremental upsert. Reviews and images have no natural key from the source anyway,
  so even a "real" upsert would still need to delete-and-reinsert those specific
  rows — wipe-and-reload just applies that same logic uniformly, and it needs to be
  safe to run on every container boot regardless.
- **Split into two independently triggerable flows** (sync, reindex) instead of one
  combined pipeline, specifically to reinforce that MySQL is the actual source of
  truth: the reindex flow has no HTTP client wired into it at all, so it's
  structurally incapable of reaching dummyjson, not just conventionally discouraged
  from it. The trade-off: running only the sync flow leaves Elasticsearch stale
  until reindex also runs — acceptable since the real deployment always enables both
  together, and the decoupling buys real operational flexibility (e.g. rebuilding
  the search index alone after a mapping change, without re-fetching from dummyjson).
- **dummyjson's pagination is handled explicitly** — the default response caps at 30
  products, so ingestion fetches everything in one call via `limit=0` (a documented
  dummyjson feature) rather than a manual limit/skip loop.
- **Coroutines are used where they're structurally correct, not by default**: the
  two independent dummyjson HTTP calls (categories, products) run concurrently via
  `async`/`await`. The MySQL write and the Elasticsearch read-back stay strictly
  sequential — each is a single `@Transactional` operation backed by one
  non-thread-safe Hibernate session, so parallelizing them would be a correctness
  bug, not an optimization. The reindex flow uses no coroutines at all: one MySQL
  read followed by one Elasticsearch write has no independent work worth overlapping.
- **`created_at` uses the source's own timestamp** (dummyjson's `meta.createdAt`),
  never fabricated. **`updated_at` is set to the sync run's own timestamp** instead —
  it tracks when this system last wrote the row. In this submission that only ever
  happens during ingestion (the API is read-only), but the semantics are the general
  correct ones for the column regardless.
- **Mapping code (raw DTO → entity → search document) is written as Kotlin
  extension functions** (`dto.toEntity(...)`, `product.toDocument(tags)`), not a
  static mapper class — it reads at the call site as "this value becomes that."

### REST API

- **Read-only, by design, not by omission.** The assignment specifies five GET
  endpoints; none of them are writes. No create/update/delete endpoints exist or
  were planned — the catalog's only writer is the ingestion pipeline.
- **List results and single-product detail deliberately serve different amounts of
  data**, mirroring the MySQL/Elasticsearch split at the API layer: `GET /products`
  returns the same flat fields the search index holds; `GET /products/{id}` returns
  everything else (dimensions, warranty/shipping/return text, sku, barcode, reviews,
  images), read fresh from MySQL.
- **`GET /products` supports more than the assignment's baseline.** `query`,
  `category`, `page`, and `size` are the required set. `brand`, `minPrice`/
  `maxPrice`, `sort`, `minRating`, and `inStock` are additional filters layered on
  top of the same composable query builder — enhancements, not baseline
  requirements (see the API reference below for which is which).
- **Input validation went through two real bugs before landing on its current
  shape**, both found by manual testing rather than anticipated upfront: an invalid
  `size` (e.g. `0`) crashed with a raw 500 because `PageRequest.of()`'s own
  `IllegalArgumentException` isn't auto-mapped to 400 by Spring MVC by default; and
  a valid-looking but too-large request (e.g. `size=50000`, or a large `page`
  combined with a normal `size`) crashed the same way because it exceeded
  Elasticsearch's `index.max_result_window`. I considered just catching every
  exception generically instead of chasing each one down, and decided against it —
  client errors (4xx) and server errors (5xx) carry different meaning that callers
  rely on, and a catch-all can't tell them apart. The actual fix was to declare the
  valid input shape upfront: `page`/`size` bounds are now plain Jakarta Bean
  Validation (`@Min`/`@Max` on the request parameters), which Spring maps to 400
  automatically with no custom code. One genuine trap along the way: adding
  `@Validated` on the controller class — the "obvious" way to enable this — actually
  activates Spring's *older* method-validation mechanism, which throws a different
  exception that *isn't* auto-mapped to 400. Removing it and letting Spring's newer,
  built-in per-parameter validation handle it alone was the actual fix. The one rule
  that can't be expressed as a simple per-field bound — `page*size+size` staying
  under Elasticsearch's limit — is a cross-field business rule, handled explicitly
  and still mapped to 400 via a small `IllegalArgumentException` handler.
- **A blank `query`/`category`/`brand` parameter is treated as if it were absent**,
  not as its literal empty-string value. Elasticsearch's query/term matching treats
  an empty string as zero search terms and matches *zero* documents, not "match
  everything" — surprising behavior for any client that sends `""` rather than
  omitting the parameter (e.g. a search box left blank).

### Infrastructure

- **`ddl-auto: validate`** — the hand-written `schema.sql` is the single source of
  truth for the schema; Hibernate checks the entity mappings against it at boot but
  is never allowed to create or modify tables itself.
- **`open-in-view: false`** — avoids the open-session-in-view anti-pattern (hidden
  N+1 queries, database connections held open across an entire request); fetching
  is deliberate in the service layer instead.
- **Lean dependency list** — 4 Spring starters to begin with, 3 more added later,
  each for a concrete, load-bearing reason: `spring-boot-starter-restclient` (Boot
  4.1 moved `RestClient` auto-configuration into its own module), `kotlinx-coroutines-core`
  (for the one place concurrency genuinely helps, above), and
  `spring-boot-starter-validation` (to replace ad-hoc manual checks with declarative
  `page`/`size` bounds, after the validation bugs above). Nothing speculative.
- **The app's Dockerfile is a multi-stage build** — a JDK image compiles and
  packages the jar, a JRE image just runs it, keeping the final image smaller with
  no compiler needed at runtime. Dependency resolution runs in its own Docker layer
  before the source is copied in, so it's only invalidated when the build file
  changes, not on every source edit.
- **Container-hostname wiring is a dedicated Spring profile**
  (`application-docker.yaml`) that overrides only the two connection URLs
  (`mysql`/`elasticsearch` hostnames instead of `localhost`) — everything else
  inherits from the base configuration unchanged.
- **The `app` compose service waits for MySQL and Elasticsearch to report healthy**
  before starting at all (`depends_on: condition: service_healthy`), not just for
  their containers to be running — this, combined with ingestion's wipe-and-reload
  idempotency, is what makes a single `docker compose up` reliably self-populating
  on both a first run and every subsequent restart.

## API reference

### `GET /categories`

Lists all categories from MySQL.

```
curl http://localhost:8080/categories
```

### `GET /products/{id}`

Full detail for a single product, from MySQL — includes dimensions, warranty/
shipping/return text, sku, barcode, reviews, and images, none of which are in the
search index. Returns `404` if the id doesn't exist.

```
curl http://localhost:8080/products/1
```

### `GET /products`

List, search, and filter products — Elasticsearch-backed. All parameters are
optional and composable.

**Assignment-required:**

| Param | Meaning |
|---|---|
| `query` | Full-text search over title/brand/tags/description, with typo tolerance |
| `category` | Exact filter by category slug |
| `page`, `size` | Pagination (`size` capped at 100) |

**Additional (enhancements beyond the brief):**

| Param | Meaning |
|---|---|
| `brand` | Exact filter by brand name |
| `minPrice`, `maxPrice` | Inclusive price range, either or both independently |
| `minRating` | Inclusive minimum rating (0–5) |
| `inStock` | `true` restricts to in-stock items; `false`/omitted applies no filter |
| `sort` | `price_asc`, `price_desc`, or `rating_desc` — overrides relevance ordering when present |

```
curl "http://localhost:8080/products?query=phone"
curl "http://localhost:8080/products?category=groceries&minPrice=1&maxPrice=50&sort=price_asc"
curl "http://localhost:8080/products?brand=Apple&inStock=true"
```

## Known limitations & extensions

- **Read-only API, deliberately** — the assignment brief specifies five read
  endpoints; no write endpoints exist or were planned.
- **Reviewer identity isn't normalized** into its own table — the source data has no
  stable user id, so inventing one would fabricate a relationship that doesn't exist.
  Reviewer name/email are kept as plain columns on the review itself.
- **Warranty/shipping/return information is kept as free text**, not parsed into
  structured fields (e.g. a duration + unit) — avoided to sidestep fragile
  text-parsing under time constraints; a production system would model this properly.
- **Prices are currency-agnostic decimals** — the source provides no currency field,
  and adding one would mean fabricating data that doesn't exist. A production schema
  would add an ISO 4217 currency column with conversion handled at the application layer.
- **Elasticsearch's `xpack.security` is disabled** for local/demo use — TLS and
  authentication would be enabled in a production deployment.
- **Only the standard analyzer is used** — no synonym handling or autocomplete
  (edge n-grams). See the fuzziness behavior note in Design decisions above for a
  concrete example of what this does and doesn't cover.
- **Ingestion validation is intentionally light** (skip and log a malformed row
  rather than fail the whole run) — the source is a trusted, fixed dataset, not
  untrusted user input, so heavier validation wouldn't add real safety here.
- **A few possible search extensions were deliberately not built**: a tags filter, a
  minimum-discount filter, and natural-language query parsing (e.g. "perfumes under
  $10"). The structured params that do exist (`minPrice`/`maxPrice`, etc.) are the
  right interface for this; true query understanding is its own subsystem and out of
  scope here.
- **The two ingestion flows can drift apart if only one is triggered** — running just
  the MySQL sync leaves Elasticsearch stale until the reindex flow also runs. The
  real deployment always enables both together, so this is a property of the
  decoupling (chosen for operational flexibility), not a bug.
- **The `app` compose service has no Docker healthcheck of its own** — nothing else
  in the compose file depends on it, so `depends_on: condition: service_healthy` on
  its own two dependencies (MySQL, Elasticsearch) is what actually matters for
  correct startup ordering.
