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

- Kotlin + Spring Boot 3.x, Gradle Kotlin DSL, JDK 17+
- Spring Web, Spring Data JPA, MySQL Driver, Spring Data Elasticsearch (only these 4)
- Config in `application.yml` (not .properties). `ddl-auto: validate` — Hibernate
  must NEVER create/modify tables; `schema.sql` (hand-written) is the source of truth.
  `open-in-view: false` deliberately.
- MySQL 8.0 + Elasticsearch 8.14.0 run via docker-compose (already working, with
  healthchecks). App runs from IntelliJ during dev against localhost ports.
  App gets its own Dockerfile + compose service ONLY at the very end.

## Current status

DONE:
- MySQL schema designed, finalized, loaded into the running container (7 tables)
- docker-compose.yml for mysql + elasticsearch (with healthchecks) working
- Spring Boot project created, application.yml fully configured (datasource, JPA,
  ES, logging), boots cleanly against both containers
- Git repo initialized, connected to public GitHub remote, 4 commits done
  (project setup / schema.sql / docker-compose / application.yml)

PENDING (in order):
1. JPA entities for the 7 tables
2. Ingestion: fetch dummyjson (paginated!) -> MySQL -> flattened docs -> ES
3. REST endpoints
4. App Dockerfile + compose `app` service (profile for container hostnames)
5. README (written by you, from the ledger below)

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

Entity-mapping traps to handle correctly: nullability must match DDL exactly;
availability_status via @Enumerated(EnumType.STRING) (never ORDINAL);
DECIMAL columns as BigDecimal (never Double); product_tags composite key via
@EmbeddedId or @IdClass.

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

## Ingestion script requirements

- dummyjson paginates: default returns only 30 products. MUST fetch all
  (limit/skip loop or limit=0). This is a known gotcha; handle explicitly.
- Flow: fetch -> upsert MySQL (normalized: resolve/create categories, brands, tags)
  -> read back with joins -> build flat docs -> bulk index to ES.
- Idempotent re-runs (decide: wipe-and-reload is acceptable; document the choice).
- Map display strings to enum codes ("In Stock" -> IN_STOCK).
- Basic sanity checks (skip+log malformed rows); no heavy validation — trusted
  source data. No CHECK constraints, no review-date assertions (deliberate).

## Naming conventions (decided)

Project/artifact: ecommerce-api · package: dev.aryan.ecommerce · 
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
  30 products; all products are fetched).
- Ingestion idempotency: state the chosen strategy (wipe-and-reload or upsert)
  and why.
- ddl-auto: validate — hand-written schema.sql is authoritative; Hibernate
  verifies mappings at boot but never mutates the schema.
- open-in-view: false — avoids the OSIV anti-pattern (hidden N+1s, connections
  held across the whole request); fetching is deliberate in the service layer.
- Lean dependency list (4 starters) — nothing speculative.

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
- Validation is light (skip+log malformed rows at ingestion) — the source is a
  trusted fixed dataset, not untrusted user input.
- Possible extensions not built (by choice, not oversight): tags filter,
  minDiscount filter, natural-language query parsing ("perfumes under $10") —
  structured params (minPrice/maxPrice etc.) are the right interface; query
  understanding is a dedicated NLP subsystem out of scope for this assignment.

README tone: plain and factual, first person, short rationale per point — not
marketing language. Structure: How to run · Architecture overview · Design
decisions · API reference (endpoints + params) · Known limitations & extensions.
