# Changelog

Notable changes to Biodex, newest first. Add your feature's entry at the top when you merge.

### Added: species data API layer (ALA)

A new `com.biodex.api` package serves species and occurrence data from the Atlas of Living Australia.
`ServiceFactory.speciesService()` returns the shared `SpeciesService`, whose Javadoc covers the
threading and failure rules.

| Method | Returns |
| --- | --- |
| `autocomplete(query, limit)` | `List<SpeciesSummary>` |
| `profile(guid)` | `SpeciesProfile` |
| `occurrencesNear(name, lat, lon, radiusKm, limit)` | `List<OccurrencePoint>` |
| `densityByArea(name, facetField)` | `List<AreaCount>` |

Components:

- `SpeciesService`, the interface every page codes against. All four methods block by design.
- `FakeSpeciesService`, three hardcoded invasive species for offline work. An `OFFLINE` constant in
  `ServiceFactory` switches it in.
- `CachedSpeciesService`, which holds species lookups for 24 hours and occurrence queries for 1 hour,
  falls back to stale data when the network fails, and never throws at a controller.
- `AlaClient` and `AlaEndpoints`, the only classes that hold a URL or read JSON. 10s timeouts on the
  JDK `HttpClient`.
- Four immutable DTOs in `com.biodex.api.dto`: `SpeciesSummary`, `SpeciesProfile`, `OccurrencePoint`,
  `AreaCount`.
- `api_cache` table plus `ApiCacheDao`.
- Gson 2.11.0, the one new dependency.
- 27 tests covering cache hits, expiry, stale fallback and the empty cache.

`api_cache` was the only schema change. `SceneRouter`, `Route`, `BaseController`, `SessionManager`,
`Main`, every FXML and every stylesheet stay as you left them.

#### Commits

| | Message |
| --- | --- |
| 1 | `chore(deps): add gson for json parsing` |
| 2 | `feat(db): add api_cache table` |
| 3 | `feat(api): add data transfer objects` |
| 4 | `feat(api): add species service interface and fake implementation` |
| 5 | `feat(api): add ala http client` |
| 6 | `feat(api): add cached ala service and factory` |
| 7 | `test: add api layer tests` |
| 8 | `docs: document api usage for page owners` |

19 files added, 1,837 lines. 72 tests pass with no network access.

#### Known gap

The brief gave us the autocomplete and occurrence-search endpoints but none for a single species
profile, so I derived that URL from the autocomplete base and never checked it against the live
service. Two nearby guesses sit in `AlaClient`: where the response holds the description, and
`isInvasive`, which matches on the words "invasive" and "pest" because the Atlas publishes no single
flag.

The cache layer swallows network errors by design, so a wrong URL returns a null profile and no
error. Nobody will catch it until Species Details renders blank fields. One live call settles it.
