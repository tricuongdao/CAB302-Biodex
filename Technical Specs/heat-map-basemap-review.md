# Geographic basemap change

This change belongs on `feature/heat-map-basemap`, based on `feature/heat-map-ui-integration` (which depends on `feature/heat-map-sightings-data`). Review its diff against `feature/heat-map-ui-integration`; the earlier branches do not need to be merged to review or run it.

## Behaviour

A local Leaflet 1.9.4 page in JavaFX WebView supplies OpenStreetMap streets, pan/zoom controls and attribution. Navigation is constrained to Greater Brisbane, with Reset map and Retry map actions. Tile coverage extends across the entire visible viewport, including the surrounding area. Leaflet uses 2D positioning because JavaFX WebView can mispaint 3D-transformed tiles after zooming. The legend stays at its preferred height and decorative overlays let pointer events through to the map controls. JavaFX still renders the existing numbered Circle hotspots, density styles, tooltips and selected-area details. Leaflet projects each hotspot's actual coordinates for the current viewport; overlays are clipped to the map and repositioned on move, zoom and resize. SQLite queries, filters, grouping, counts and the existing bounds validator are unchanged. Hotspots still represent the stored suburb coordinates rather than precise individual sighting locations.

Only tile coordinates are sent to the tile provider. Sighting descriptions, species, identities and database records remain outside the WebView. The page bundles its scripts/styles, limits network content with a Content Security Policy, identifies the app with a Biodex user agent, and opens only the two attribution destinations in the system browser. Leaflet's BSD licence is included in `src/main/resources/com/biodex/map/vendor/LICENSE`; assets are unmodified upstream 1.9.4 distribution files from https://unpkg.com/leaflet@1.9.4/dist/ (JS/CSS/images). The default layer and marker images are included to keep the distribution complete, although the current screen uses JavaFX Circle markers.

Internet is required for uncached street tiles. On tile failures or a 12-second loading timeout, a status message explains the issue while saved sightings and filtering remain usable on the geographic canvas. Retry reloads the local page and resets the viewport. If the local page itself cannot initialise, approximate positions use the original plain-canvas projection. No bulk tile download, cache bypass, geocoder, or API key is introduced. Public OSM tiles have no availability guarantee; a production deployment should review provider suitability and caching behaviour.

References: [Leaflet API](https://leafletjs.com/reference.html), [OSM tile policy](https://operations.osmfoundation.org/policies/tiles/).

## Verification

- `sh loader/mvnw test` runs the existing suite without needing a display or network map access.
- `sh loader/mvnw test -Dbiodex.test.webview=true` also opens a temporary desktop window to check bundled page startup, Brisbane projection, zoom callbacks, reset, resize and retry. This requires a graphical desktop. It also loads the actual FXML screen against an in-memory SQLite fixture, checks that decorative overlays cannot intercept map controls, verifies marker alignment through both zoom directions, and checks tile coverage at the viewport edges. It does not assert external tile availability.
- For an optional live-network rendering check, add `-Dbiodex.test.tiles=true`. This waits for tile images and checks the rendered viewport for large blank areas, catching painting failures that DOM-only checks miss. Add `-Dbiodex.test.snapshot=/tmp/biodex-map.png` to save a full-screen PNG for visual review.
- In IntelliJ, reload Maven to resolve `javafx-web` at the same version as the existing JavaFX modules, then run the normal application.

Manual review: open Heat map with saved sightings; inspect a numbered hotspot; switch date/species filters and clear them; pan, zoom and resize while checking that markers stay over their suburbs; reset the map; disconnect the network, navigate to uncached tiles and check the status and filters; reconnect and retry. Check both themes and that the attribution remains visible/clickable. No schema changes or seeded sightings are required by this change.
