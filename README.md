# Rootly backend

Spring Boot 4 / Java 17 API for fetching real Sri Lankan heritage places for
Explore Places. The current endpoint reads Wikidata, not demo records or a
database. It does not require a paid API key. The Flutter province detail page also uses this service for province-specific
heritage sites, traditions and source-linked photos.

## Project structure and conventions

The backend follows the applicable layers and naming conventions in cdb-merchant:

```text
src/main/java/com/backend/rootly/
  advisor/          HTTP exception handling
  client/           External provider clients (Wikidata)
  config/           Spring beans, security and CORS configuration
  controller/       HTTP mappings and request/response handling
  domain/           Service request models and catalog data
  dto/request/      Request DTOs
  dto/response/     Response DTOs
  enums/            Supported categories
  exception/        Application exceptions
  filter/           Request tracing, request logging and JWT authentication
  mapper/           Provider-to-DTO mapping
  service/          Service interfaces
  service/impl/     Business logic and cache implementations
  utility/          Shared constants, including EndPoint
src/main/resources/queries/  Provider queries
src/test/java/com/backend/rootly/  Tests mirroring the application packages
```

Use PascalCase class names and DTO, Service, ServiceImpl, Client, and Mapper
suffixes for their respective roles. Controllers inject service interfaces through
constructors and return ResponseEntity<Object> values. Keep endpoint paths in
utility.EndPoint, including references from security and CORS configuration.
Keep provider I/O in client, mapping in mapper, business logic in service/impl,
and HTTP error translation in advisor. Controllers map validated request DTOs to
domain request classes using the shared ModelMapper bean before calling services.
Use plain @Validated with default constraints; no validation groups are required.

Use explicit imports, four-space indentation, Lombok-generated JavaBean accessors
and constructors, and parameterized logging through @Log4j2. DTOs and the catalog
use @Data, @NoArgsConstructor, and @AllArgsConstructor. Spring components use
@RequiredArgsConstructor with final dependencies. ExploreProperties binds the
rootly.explore settings through @ConfigurationProperties and validates cache
settings at startup with @PostConstruct. Provider query construction belongs in
configuration, while cache age checks belong in the service. Timestamps use
UTC Instant consistently. Custom exception constructors remain explicit because
they initialize the superclass message/cause. The Java 17 compiler warnings and
PMD checks remain enabled.

Add dao and entity packages when persistence is implemented, and add annotation,
validator, or filter packages when those features need them. Empty sample
classes are removed. Rootly uses POST for places searches and GET for categories,
with problem JSON errors. Merchant-specific endpoints, response envelopes, and
infrastructure are not part of this structural refactor.

## Run

From `Rootly_Backend`, with JDK 17 or newer installed:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux/macOS use `./mvnw spring-boot:run`. The server listens on port 8080.
The first places request loads the public catalog and can take several seconds.
The categories endpoint does not contact the provider. Opening a place detail fetches its English Wikipedia introduction and lead photo when that place has a linked article, then caches the result for 15 minutes. If Wikipedia has no matching article or is unavailable, the detail endpoint returns the existing Wikidata description and image.

## Endpoints

```http
POST /api/v1/explore/places
Content-Type: application/json

{"q":"temple","category":"sacred-sites","page":0,"size":10}

GET /api/v1/explore/categories

GET /api/v1/explore/places/{placeId}

POST /api/v1/explore/province
Content-Type: application/json

{"provinceId":"uva","q":"temple","page":0,"size":10}
```

The province endpoint accepts one of Sri Lanka's nine province IDs (`northern`,
`north-central`, `north-western`, `central`, `eastern`, `western`, `southern`,
`sabaragamuwa`, `uva`). It returns a sourced `province` profile, a paged
`places` object with the same pagination/provenance fields as Explore Places,
and a `traditions` array. Optional `q` searches that province's place names, locations,
categories and descriptions (up to 120 characters, case/accent-insensitive).
The filter runs before pagination; `total` and `hasNext` describe matching places.
Traditions are unaffected. Image URLs point to Wikimedia Commons; corresponding
`imageSourceUrl` values open the source file pages. Missing source photos or
traditions remain empty, and cached data is marked `stale` when the provider
cannot be refreshed. Photos are fetched by the Flutter app as 900px Commons
thumbnails for faster loading.

The places endpoint requires a JSON body. Send an empty object to use all
defaults. The previous GET places route has been replaced by POST.

| JSON field | Default | Meaning |
| --- | --- | --- |
| `q` | empty | Up to 120 characters; case/accent-insensitive search across name, location, category and description. All search words must match. |
| `category` | `all` | `all`, `ancient-ruins`, `sacred-sites`, `museums`, or `heritage-sites`. |
| `page` | `0` | Zero-based page, maximum 1,000,000. |
| `size` | `20` | Page size, 1–50. |

Response fields:

- `items`: matching places, sorted by name then stable Wikidata ID.
- Each place: `id`, `name`, `subtitle`, `category`, `categoryId`,
  `location: {latitude, longitude}`, `description`, `imageUrl`, `imageSourceUrl`,
  `sourceUrl`, and `wikipediaUrl`.
- `page`, `size`, `total`, `hasNext`: pagination over the loaded catalog.
- `source`, `fetchedAt`, `stale`, `truncated`: provenance and cache status.

No matches produce HTTP 200 with an empty `items` array, not unrelated fallback
places. Missing optional descriptions/images/links remain null. Ratings, opening
hours and ticket prices are not supplied because this source does not reliably
provide them. Categories are source classifications, not historical eras.
The `subtitle` area can be a district, province, city or other administrative
area; do not treat it as a normalized province ID.

Invalid or missing JSON request bodies return HTTP 400 with a problem JSON body. An upstream outage
with no usable cached catalog returns HTTP 503 and `Retry-After`.

## Configuration

| Environment variable | Default |
| --- | --- |
| `PORT` | `8080` |
| `WIKIDATA_URL` | `https://query.wikidata.org/sparql` |
| `WIKIPEDIA_API_URL` | `https://en.wikipedia.org/w/api.php` |
| `WIKIDATA_USER_AGENT` | `RootlyBackend/0.1 (Sri Lanka heritage explorer)` |
| `EXPLORE_CONNECT_TIMEOUT` | `5s` |
| `EXPLORE_REQUEST_TIMEOUT` | `25s` |
| `EXPLORE_CACHE_TTL` | `15m` |
| `EXPLORE_MAX_STALE` | `24h` |
| `ROOTLY_ALLOWED_ORIGINS` | `http://localhost:*,http://127.0.0.1:*` |
| `ROOTLY_LOG_PATH` | `logs` |
| `ROOTLY_LOG_LEVEL` | `debug` |
| `ROOT_LOG_LEVEL` | `info` |
| `REQUEST_LOG_MAX_PAYLOAD` | `8192` bytes |


Each HTTP response includes `X-Request-Id`. Request logs include that ID, the
`X-B3-TraceId` value when supplied, method, URI, response status and duration.
JSON, text and form bodies are logged at DEBUG up to the configured payload
limit. Authorization, cookies, API keys, passwords, tokens, secrets and OTPs are
redacted. Actuator requests are excluded. Log4j2 writes separate rolling DEBUG,
INFO, WARN and ERROR files under `ROOTLY_LOG_PATH`; files rotate daily or at 10 MB.

Set a descriptive User-Agent with a real project URL or maintainer contact before
deployment. For Flutter web, set `ROOTLY_ALLOWED_ORIGINS` to your site's origin;
comma-separated origins are supported. POST places, GET place detail, GET categories, and POST province are public;
other application paths remain denied by Spring Security. CORS permits GET and POST
from configured origins. The public, read-only places POST is exempt from CSRF
checks; other paths retain CSRF protection.

The cache is in memory, shared by all searches within a server process. Refreshes
are serialized, and failures back off for at least 60 seconds, respecting the
provider's `Retry-After`. Cached real data can be served up to `max-stale` age,
with `stale: true`. A process restart clears the cache. Multiple server replicas
have independent caches; use shared storage/cache when scaling the service.

The SPARQL query selects Sri Lanka (Wikidata Q854), coordinates, and cultural
place classes or a recorded heritage designation. This includes landmarks typed
as settlements rather than attractions. Its 5,000-row safety limit is reported by `truncated`; `total`
counts the available deduplicated catalog, not every attraction in Sri Lanka.
Duplicate images/categories are merged by Wikidata ID, with specific categories
preferred over the generic heritage category. Coverage follows public source
data and is not guaranteed complete.

MongoDB auto-configuration is enabled for the user and capsule repositories. Unused
JDBC auto-configuration remains disabled.

## Flutter connection

Use these base URLs when connecting the Flutter repository:

- Windows or browser on this machine: `http://localhost:8080`
- Android emulator: `http://10.0.2.2:8080`
- Physical phone: your development computer's LAN address, port 8080.

Use HTTPS in deployment. Local Android HTTP testing also requires a development
network-security configuration and internet permission. Send the search fields as a POST JSON body. Read `items` into the
Flutter `Place` model, show loading/error/empty states, and use the source's
actual image URL instead of bundled placeholder images. The categories endpoint
provides supported filters; "Craft" is not currently a supported location class.

## Source and image attribution

Structured facts come from [Wikidata](https://www.wikidata.org/wiki/Wikidata:Data_access)
(CC0). Show the returned source links so users can inspect the original records.
Images retain their individual Wikimedia Commons licenses: `imageSourceUrl`
links to the file description containing the author and license. It is not a
blanket license to redistribute the image. Display the applicable author/license
credits when integrating photos; see
[Commons reuse guidance](https://commons.wikimedia.org/wiki/Commons:Reusing_content_outside_Wikimedia).
Place detail may return plain-text article introductions from Wikipedia, with the article link in `wikipediaUrl`; Wikipedia text is available under CC BY-SA and requires attribution. The image source link points to the file page for author and licence details. If an article has no introduction or photo, those fields may remain short or empty rather than being invented.

## Question forum API

The authenticated question forum stores questions, Reddit-style nested comments,
votes and bookmarks in MongoDB. Send the JWT returned by the login endpoint as
`Authorization: Bearer <token>` on every request.

```http
GET /api/v1/questions?q=temple&category=rituals-etiquette&sort=top&page=0&size=20
GET /api/v1/questions/{questionId}?commentSort=top

POST /api/v1/questions
Content-Type: application/json

{
  "title": "Why are lotus flowers offered at sacred places?",
  "body": "I saw families carrying white lotus flowers and would like to understand the meaning.",
  "location": "Anuradhapura",
  "category": "rituals-etiquette"
}

PUT /api/v1/questions/{questionId}
Content-Type: application/json

{
  "title": "Updated question title",
  "body": "Updated question context with enough detail.",
  "location": "Anuradhapura",
  "category": "rituals-etiquette"
}

DELETE /api/v1/questions/{questionId}

POST /api/v1/questions/{questionId}/comments
Content-Type: application/json

{"body":"A top-level answer","parentCommentId":null}

POST /api/v1/questions/{questionId}/comments
Content-Type: application/json

{"body":"A nested reply","parentCommentId":"comment-id"}

PUT /api/v1/comments/{commentId}
Content-Type: application/json

{"body":"Updated comment text"}

DELETE /api/v1/comments/{commentId}

PUT /api/v1/questions/{questionId}/vote
PUT /api/v1/comments/{commentId}/vote
Content-Type: application/json

{"value":1}

PUT /api/v1/questions/{questionId}/bookmark
DELETE /api/v1/questions/{questionId}/bookmark
```

Vote values are `1` for upvote, `-1` for downvote, and `0` to remove the
current user's vote. Question authors, comment authors, viewer votes, bookmarks
and ownership flags are derived from the authenticated user rather than accepted
from the request body. Only an item's owner may edit or delete it. Question
deletion is soft deletion. A deleted comment with active replies remains as a
content-free tombstone so the nested conversation is preserved; a deleted leaf
comment is omitted from the response.

## Offline translation API

The authenticated translation API uses a curated MongoDB word bank and does not
call Google, Azure, or another translation provider. On startup it inserts any
missing entries from `src/main/resources/data/translations.json`; existing entries
are left unchanged. The initial catalogue has 60 English-Sinhala entries across
Temple, Greetings, Food, and Directions.

```http
POST /api/v1/translations/lookup
Authorization: Bearer <token>
Content-Type: application/json

{"text":"stupa","sourceLanguage":"en","targetLanguage":"si"}

GET /api/v1/translations/glossary?category=Temple&page=0&size=10
Authorization: Bearer <token>
```

Lookup supports `en` and `si` in either direction. English matching is
case-insensitive and accent-insensitive, and also checks curated aliases. Sinhala
matching checks the Sinhala word, transliteration, and pronunciation guide. An
unknown word returns HTTP 404 rather than an invented translation. Set
`TRANSLATION_SEED_ENABLED=false` to disable startup seeding.

## Verify

```powershell
.\mvnw.cmd verify
```

Tests use isolated fixtures and a local HTTP provider stub. They do not need
internet access or a live Wikidata request. Production never uses those fixtures.
