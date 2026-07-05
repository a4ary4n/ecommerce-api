# CLAUDE.md — Project Context & Working Instructions

## What this project is

Take-home assignment for a Backend Developer interview: a simple e-commerce REST API
using **Kotlin + Spring Boot**, **MySQL** (source of truth) and **Elasticsearch**
(derived search index), with data ingested from https://dummyjson.com/products.
Must be runnable out-of-the-box with a single `docker compose up`.

Submission = full codebase + README explaining how to run it, design choices,
trade-offs and known limitations. The README will be written by you (Claude Code)
at the end — the "README ledger" section below accumulates everything it must cover.

## How to work with me (IMPORTANT)

- I am an experienced Android/Kotlin developer relearning backend. **All design
  decisions below were made deliberately by me — do not silently change them.**
  If you think a decision is wrong, say so and explain, but wait for my agreement.
- I want to understand everything that goes into this project. When generating
  code, briefly explain non-obvious choices (annotations, Spring conventions,
  ES query structure). Assume strong Kotlin knowledge, zero Spring/ES experience.
- Prefer minimal dependencies. Do not add libraries beyond what's listed without asking.
- I will review all generated code. Keep changes small and reviewable.

## Git conventions (follow exactly)

- **Never commit on your own initiative. I decide when to commit — only commit
  when I explicitly say so, and always show me the diff / ask for review first.**
- Commit messages: short, lowercase, imperative, plain — NO prefixes like
  feat:/chore:/fix:. Match the existing style:
  - `initial Spring Boot project setup`
  - `MySQL schema for products, categories, brands, reviews, tags`
  - `docker-compose for MySQL and Elasticsearch with healthchecks`
  - `configure datasource, JPA, Elasticsearch`
- One logical unit per commit. Repo is public on GitHub (origin already set).

## Stack (decided)

- Kotlin + Spring Boot 4.1.0, Gradle Kotlin DSL, JDK 17+
- Spring Web, Spring Data JPA, MySQL Driver, Spring Data Elasticsearch — the
  original 4. Two more added during ingestion work, each for a concrete reason:
  - `spring-boot-starter-restclient` — Boot 4.1 moved `RestClient`
    auto-configuration into this separate module; `spring-boot-starter-webmvc`
    alone doesn't pull it in (confirmed via jar inspection).
  - `kotlinx-coroutines-core` — used deliberately, not decoratively (see
    Ingestion section below for exactly where and why).
- Config in `application.yml` (not .properties). `ddl-auto: validate` — Hibernate
  must NEVER create/modify tables; `schema.sql` (hand-written) is the source of truth.
  `open-in-view: false` deliberately.
- MySQL 8.0 + Elasticsearch 9.4.3 run via docker-compose (already working, with
  healthchecks). App runs from IntelliJ during dev against localhost ports.
  (Bumped from 8.14.0: Spring Boot 4.1's managed Spring Data Elasticsearch 6.1.0
  bundles Elastic's Java client v9.4.2, which only speaks to same-major-version
  servers — Elastic's compatibility policy allows a server to accept one major
  version behind, never a server serving a newer-major client.)
  App gets its own Dockerfile + compose service ONLY at the very end.
- `schema.sql` is mounted into MySQL's `/docker-entrypoint-initdb.d/` in
  docker-compose.yml, so it auto-applies on a genuinely fresh volume (was
  previously loaded by hand — a gap found while tracing what a grader's first
  `docker compose up` would actually do).

## Current status

DONE:
- MySQL schema designed, finalized (7 tables); schema.sql auto-applies via
  docker-compose's MySQL init mount (fresh volumes only)
- docker-compose.yml for mysql + elasticsearch (with healthchecks) working;
  Elasticsearch on 9.4.3 (see Stack section for why)
- Spring Boot project created, application.yml fully configured (datasource, JPA,
  ES, logging), boots cleanly against both containers
- JPA entities for all 7 tables (`entity/` package), verified against the live
  schema via `ddl-auto: validate`
- Ingestion pipeline complete and verified end-to-end against the live
  containers (194/194 products, both directions, idempotent) — see Ingestion
  section below for the full design
- Git repo, connected to public GitHub remote, several commits done (project
  setup / schema.sql / docker-compose / application.yml / JPA entities / ...)

PENDING (in order):
1. REST endpoints
2. App Dockerfile + compose `app` service (profile for container hostnames;
   sets `APP_INGEST_MYSQL_ENABLED=true` and `APP_INGEST_ELASTICSEARCH_ENABLED=true`
   so both ingestion flows run automatically on `docker compose up`)
3. README (written by you, from the ledger below)

## MySQL schema — 7 tables (FINAL, do not alter without discussion)

categories(id PK, slug UNIQUE, name) — no url column: derivable from slug
brands(id PK, name UNIQUE) — products.brand_id is NULLABLE (source has brandless products)
products(id PK = dummyjson's own id, title, description TEXT, category_id FK NOT NULL,
  brand_id FK NULL, sku UNIQUE, price DECIMAL(10,2), discount_percentage DECIMAL(5,2),
  rating DECIMAL(3,2), stock, availability_status ENUM('IN_STOCK','LOW_STOCK','OUT_OF_STOCK'),
  weight/width/height/depth DECIMAL(8,2), warranty_information VARCHAR(255),
  shipping_information VARCHAR(255), return_policy VARCHAR(255),
  minimum_order_quantity, thumbnail VARCHAR(500), barcode VARCHAR(50),
  qr_code VARCHAR(500), created_at, updated_at)
reviews(id PK autoincr, product_id FK, rating TINYINT, comment TEXT,
  reviewer_name, reviewer_email, review_date)
product_images(id PK autoincr, product_id FK, url VARCHAR(500), sort_order)
tags(id PK, name UNIQUE)
product_tags(product_id, tag_id — composite PK, both FK)

Entity-mapping traps handled (confirmed via `ddl-auto: validate` against the live
schema, not just by inspection): nullability matches DDL exactly, including
columns with a `DEFAULT` but no `NOT NULL` (nullable in Kotlin too, since
Hibernate always writes explicit values and ignores DB defaults anyway);
availability_status via @Enumerated(EnumType.STRING) (never ORDINAL); DECIMAL
columns as BigDecimal (never Double); product_tags composite key via
@EmbeddedId + @MapsId (not @IdClass); TINYINT -> Byte, SMALLINT -> Short (not
both mapped to Int — schema validation caught this); MySQL TEXT columns need
@JdbcTypeCode(SqlTypes.LONGVARCHAR), not @Lob (which expects LONGTEXT and fails
validation against a plain TEXT column).

## Elasticsearch design (FINAL)

Index: `products`. Documents are FLAT and DENORMALIZED, built by reading back from
MySQL after insert (never a parallel fetch from dummyjson) — guarantees MySQL and
ES agree. ES doc contains ONLY search-relevant fields: id, title, description,
category (name/slug), brand (name), tags (array), price, discount_percentage,
rating, stock, availability_status, thumbnail, created_at.
NOT in ES: reviews, images, dimensions, warranty/shipping/return, sku, barcode
(full detail served by /products/{id} from MySQL).

Mapping decisions:
- title: text + .keyword subfield
- description: text only (no keyword — deliberate, prose is never exact-matched)
- category: keyword ONLY (filter-only; excluded from full-text search deliberately —
  including it would drown ranking, e.g. "fragrance" matching every product in category)
- brand, tags: text + .keyword multi-field (searched as prose AND filtered exactly) —
  the dual-behavior multi-field pattern is intentional
- price/rating/discount: double (ES is derived index; DECIMAL-vs-double asymmetry
  with MySQL is deliberate), stock: integer
- availability_status: keyword (the machine codes)
- thumbnail: keyword with index:false (display payload, never queried)
- Standard analyzer; NO custom analyzers/synonyms/autocomplete (deliberately out
  of scope)

## Search behavior (FINAL)

- ?query= -> multi_match over title^4, brand^3, tags^2, description^1,
  fuzziness AUTO
- ?category= -> term filter on category keyword (filter context, no scoring)
- query+category combine: bool { must: [multi_match], filter: [term] }
- Extra params (Tier 1, build): page/size (ES from/size), sort=price_asc|price_desc|
  rating_desc (sort replaces _score; default is relevance when query present),
  brand=, minPrice=/maxPrice= (range filter)
- Tier 2 if time: minRating=, inStock=
- Deliberately NOT built: tags filter, minDiscount, NL query parsing ("perfumes
  under $10" — query-understanding is out of scope; structured params instead).
  Currency: do NOT add a currency column (see README ledger).
- One composable query builder handles all param combinations.

## Endpoints (assignment-required)

- GET /categories — list all (MySQL)
- GET /products — list all (paginated)
- GET /products/{id} — single product, full detail (MySQL)
- GET /products?query= — ES full-text
- GET /products?category= — filter (ES)
(query and category are the same endpoint with optional params, plus the extras above)

## Ingestion (FINAL, implemented and verified)

Two independently triggerable flows, not one combined pipeline — reinforces MySQL
as the actual source of truth (the ES-refresh flow's only dependency is MySQL,
never the network):

1. **`ProductCatalogSyncService.sync()`** (dummyjson -> MySQL), gated by
   `app.ingest.mysql.enabled` via `ProductCatalogSyncRunner` (a
   `@ConditionalOnProperty`-gated `CommandLineRunner`).
2. **`ProductSearchIndexService.reindex()`** (MySQL -> Elasticsearch, no network
   call at all), gated by `app.ingest.elasticsearch.enabled` via
   `ProductSearchIndexRunner`. Reads MySQL fresh and rebuilds the ES index from
   that — nothing here ever touches dummyjson.

Both runners are `@Order`-annotated (sync=1, reindex=2) so that if both flags are
set together in one boot (the eventual docker-compose `app` service does this),
sync always completes before reindex reads MySQL. Verified independently: with
only the mysql flag set, ES is untouched (confirmed by deleting the index first
and checking it stays absent); with only the elasticsearch flag set, ES is
rebuilt purely from whatever's already in MySQL.

Flow 1 (sync) details:
- dummyjson paginates (default 30 products) — fetched via `limit=0`, a documented
  dummyjson feature returning all 194 items in one call. Categories come from the
  separate `/products/categories` endpoint (the only source of proper display
  names; a product's own `category` field is just the slug).
- Idempotency: **wipe-and-reload**, confirmed as the strategy. Every sync run
  deletes all 7 tables in FK-safe order (`product_tags -> reviews ->
  product_images -> products -> categories -> brands -> tags`) and reloads fresh.
  Safe to run on every container boot; reviews/images have no natural key from the
  source anyway, so a "real" upsert would still need delete-and-reinsert for those
  regardless — wipe-and-reload just applies that uniformly.
- `products.created_at` uses dummyjson's own `meta.createdAt` (never fabricated).
  `products.updated_at` is instead set to the sync run's own current timestamp —
  it tracks when *our* system last touched the row, and will be bumped again once
  REST endpoints support modifying products (a future PUT/PATCH's job).
- Categories/brands/tags resolved via in-memory caches during the sync run (safe
  to skip a DB existence check since the tables were just wiped).
- A product with no brand (the JSON key is *absent*, not `null`) maps to Kotlin
  `null` via `jackson-module-kotlin`'s constructor-default fill-in, stored as
  `NULL` in `brand_id` — never coerced to an empty string or a sentinel brand.
- `availabilityStatus` display strings ("In Stock" etc.) mapped to the enum via
  an explicit `when`; skip+log the product if unrecognized (no heavy validation —
  trusted source data, not untrusted input).
- Money/rating/dimension `Double -> BigDecimal` conversions use
  `BigDecimal.valueOf(double)`, never Kotlin's `Double.toBigDecimal()` (which
  preserves the double's exact binary value with no rounding, producing visible
  float noise like `19.9899999999999999911182...` instead of `19.99`).
- Mapping code is Kotlin extension functions (`dto.toEntity(...)`,
  `product.toDocument(tags)`), not a static mapper object — reads at the call
  site as "this value becomes that."
- `kotlinx.coroutines`: the two independent dummyjson HTTP calls (categories,
  products) are fetched concurrently via `async`/`await`. The MySQL write itself
  stays a single sequential blocking call (just offloaded via
  `withContext(Dispatchers.IO)`) — not parallelized internally or against
  anything else, since it's a single `@Transactional` method backed by one
  non-thread-safe Hibernate `Session`.

Flow 2 (reindex) details:
- Reads `Product` + lazy `category`/`brand` via one `JOIN FETCH` /
  `LEFT JOIN FETCH` query (brand is nullable, category isn't) and all products'
  tags via one batched `IN (...)` query — two queries total regardless of product
  count, no N+1, correct under `open-in-view: false`.
- `ElasticsearchIndexer` owns the index lifecycle explicitly: drops and recreates
  the `products` index every reindex run. `ProductDocument` is annotated
  `@Document(..., createIndex = false)` — Spring Data Elasticsearch's own default
  (`createIndex = true`) auto-creates an empty index the moment the
  `ProductSearchRepository` bean is constructed, unconditionally on every app
  boot regardless of our own flags, which would silently defeat the mysql-only
  flow's guarantee of never touching ES.
- No coroutines in this flow — a single MySQL read followed by a single ES bulk
  write has no independent concurrent work to overlap, unlike flow 1's two HTTP
  calls.

## Naming conventions (decided)

Project/artifact: ecommerce-api · package: dev.aryan.ecommerceapi ·
DB: ecommerce · ES index: products · compose services: app, mysql, elasticsearch
(service names double as DNS hostnames in the compose network)

## README ledger (Claude Code writes the final README from this)

The README must contain: how to run locally (single `docker compose up`), design
choices & thought process, trade-offs & known limitations. Cover ALL of the
following points, each with its rationale — these were explicitly discussed and
decided; they are the "architectural judgment" the reviewer is grading:

Design choices:
- MySQL is the source of truth; Elasticsearch is a derived, read-optimized search
  index. Duplication between them is the intended pattern, not redundancy. ES is
  indexed by reading back from MySQL (never a parallel source fetch) so the two
  stores can never disagree.
- ES documents are flat/denormalized and contain only search-relevant fields;
  full product detail is served from MySQL via /products/{id}.
- products.id reuses dummyjson's own id — stable natural key, avoids a pointless
  remapping layer.
- DECIMAL (never FLOAT) for money/ratings in MySQL — exactness; vs double in ES
  where it's a derived index and float semantics are conventional. Same data,
  different role, different type — deliberate asymmetry.
- rating stored as given, NOT derived from the reviews table — the source's
  embedded reviews are a partial sample, not the population the rating was
  computed from; recomputing would produce wrong numbers.
- availability_status kept despite stock column — it encodes the source system's
  business logic (thresholds, possible manual overrides) not derivable from stock
  count alone; recomputing client-side risks diverging from source of truth.
- Enum values normalized to machine codes (IN_STOCK etc.), not display strings —
  stored representation decoupled from presentation; ingestion maps between them.
- categories/brands/tags normalized into their own tables (repeating shared
  values, textbook many-to-many for tags); 1:1 product attributes stay as columns
  (deliberately avoiding over-normalization).
- categories.url dropped — derivable from slug; storing it is redundancy that
  can go stale.
- URL-typed columns standardized at VARCHAR(500) across all tables ("consistent
  domain types").
- Full-text search covers title^4, brand^3, tags^2, description^1 (weighted by
  relevance signal strength: title is the most information-dense; a brand match
  is high-intent, nearly as strong as title; tags are loose thematic labels;
  description is prose). Category is deliberately EXCLUDED from full-text and is
  a filter only — including it would drown ranking (e.g. "fragrance" matching
  every product in the category via that field).
- Structured constraints (category, brand, price, rating, stock) live in ES
  filter context, not query context — no scoring cost, cacheable. query+filters
  compose into a single bool query; one query builder serves every param
  combination.
- Sorting: relevance (_score) by default when ?query= is present; explicit
  sort (price/rating) otherwise — scoring and ordering are separable concerns.
- fuzziness AUTO on multi_match — cheap typo tolerance.
- Pagination implemented (page/size) even though unspecified — a listing API
  returning the full catalog in one response isn't viable; documented as an
  assumption taken rather than a question asked.
- Ingestion handles dummyjson's pagination explicitly (default response is only
  30 products; fetched in one call via `limit=0`, a documented dummyjson feature,
  rather than a manual limit/skip loop).
- Ingestion idempotency: wipe-and-reload. Every sync run deletes all 7 tables
  (FK-safe order) and reloads fresh from dummyjson. Chosen over a "real" upsert
  because reviews/images have no natural key from the source anyway — an upsert
  would still need delete-and-reinsert for those children regardless, so
  wipe-and-reload just applies that same logic uniformly across the whole
  catalog. Needs to be safe to re-run on every container boot in the eventual
  compose deployment, which this guarantees.
- Ingestion is split into two independently triggerable flows — dummyjson->MySQL
  and MySQL->Elasticsearch — rather than one combined pipeline. Reinforces MySQL
  as the actual source of truth: the search-index-refresh flow's only dependency
  is MySQL, and it is structurally incapable of reaching dummyjson (no HTTP
  client wired into that path at all), not just conventionally discouraged from
  it. Ordered so both can still run together in one boot for the normal
  deployment path.
- `kotlinx.coroutines` used where it's structurally correct, not decoratively:
  the two independent dummyjson HTTP calls (categories, products) run
  concurrently via `async`/`await`. The MySQL write/read steps are deliberately
  NOT parallelized against each other or internally — they're `@Transactional`
  JPA operations backed by a single non-thread-safe Hibernate `Session`, so
  concurrency there would be a correctness bug, not an optimization. The
  MySQL->ES flow uses no coroutines at all — a single read then a single write
  has no independent work worth overlapping.
- Ingestion mapping code (DTO -> entity -> ProductDocument) is written as Kotlin
  extension functions, not a static mapper class — idiomatic Kotlin, reads at
  the call site as "this value becomes that" rather than a utility-class call.
- ddl-auto: validate — hand-written schema.sql is authoritative; Hibernate
  verifies mappings at boot but never mutates the schema.
- open-in-view: false — avoids the OSIV anti-pattern (hidden N+1s, connections
  held across the whole request); fetching is deliberate in the service layer.
- Lean dependency list — started at 4 starters; two more added during ingestion
  for concrete, load-bearing reasons (RestClient auto-configuration moved to its
  own Boot 4.1 module; kotlinx-coroutines for the one place concurrency
  genuinely helps) — nothing speculative.

Known limitations / trade-offs (state honestly, each is deliberate):
- Reviewer identity not normalized (no users table) — source data has no stable
  user id; inventing one would fabricate relationships. reviewer name/email kept
  as review columns.
- warranty/shipping/return kept as free VARCHAR — a production system would parse
  these into structured duration fields; not built to avoid fragile text-parsing
  under time constraints.
- Prices are currency-agnostic decimals — source provides no currency field; a
  production schema would add an ISO 4217 currency code column, with conversion
  at the application layer. No currency column was added (would fabricate data).
- xpack security disabled for local ES — TLS/auth would be enabled in production.
- Standard analyzer only — production search would add synonym handling and
  autocomplete (edge n-grams). Listed as extensions, deliberately out of scope.
  Concretely observed: `fuzziness: AUTO` scales allowed edit distance by query
  length (0 edits for 0-2 chars, 1 edit for 3-5, 2 edits for 6+) — so
  `?query=perf` and `?query=perfu` return nothing while `?query=perfum`/
  `perfume` match products tagged "perfumes" (edit distance 2 and 1
  respectively, both within the length-6+ bucket's allowance; the shorter
  queries need 3-4 edits, which exceeds their 1-edit bucket). This is
  typo-tolerance on complete-ish words, not prefix/incomplete-word
  autocomplete — those are genuinely different features, and this is expected
  behavior given the above, not a bug.
- Validation is light (skip+log malformed rows at ingestion) — the source is a
  trusted fixed dataset, not untrusted user input.
- Possible extensions not built (by choice, not oversight): tags filter,
  minDiscount filter, natural-language query parsing ("perfumes under $10") —
  structured params (minPrice/maxPrice etc.) are the right interface; query
  understanding is a dedicated NLP subsystem out of scope for this assignment.
- Splitting ingestion into two independently triggerable flows means running
  only the mysql-sync flow leaves Elasticsearch stale relative to MySQL until
  the reindex flow is also triggered — accepted because the real deployment
  path (docker-compose `app` service) always enables both together; the
  decoupling is for operational flexibility (e.g. rebuilding the search index
  alone after an ES mapping change, without re-fetching from dummyjson), not
  a claim that they auto-stay in sync.

README tone: plain and factual, first person, short rationale per point — not
marketing language. Structure: How to run · Architecture overview · Design
decisions · API reference (endpoints + params) · Known limitations & extensions.
