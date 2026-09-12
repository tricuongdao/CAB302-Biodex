/* Leaflet owns projection and navigation; JavaFX retains the hotspot Circle overlay. */
window.biodexMap = (function () {
    'use strict';
    var map, tiles, timeout;
    var bounds = [[-28.35, 152.30], [-26.80, 153.65]];
    var unavailable = 'Map tiles unavailable or incomplete. Saved sightings and filters still work. Retry map to reconnect.';
    function status(message) { window.biodexBridge.tileStatus(message); }
    function moved() { window.biodexBridge.moved(); }
    return {
        start: function () {
            map = L.map('map', {
                maxBounds: bounds, maxBoundsViscosity: 1,
                minZoom: 7, maxZoom: 18,
                zoomAnimation: false, fadeAnimation: false, markerZoomAnimation: false,
                inertia: false
            });
            document.addEventListener('click', function (event) {
                var link = event.target.closest('a');
                if (link && link.closest('.leaflet-control-attribution')) {
                    event.preventDefault();
                    window.biodexBridge.openAttribution(link.getAttribute('href'));
                }
            });
            map.on('move zoom resize', moved);
            this.reset();
            tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                // Restrict navigation above, not tile coverage: edge tiles must fill the viewport.
                maxZoom: 19,
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap contributors</a>'
            });
            tiles.on('loading', function () {
                status('Loading street map...');
                clearTimeout(timeout);
                timeout = setTimeout(function () { status(unavailable); }, 12000);
            });
            tiles.on('tileerror', function (event) { event.tile.dataset.failed = 'true'; });
            tiles.on('load', function () {
                clearTimeout(timeout);
                var failed = document.querySelector('.leaflet-tile[data-failed="true"]');
                status(failed ? unavailable : 'Drag to pan; use + / − or scroll to zoom. Sightings are grouped by suburb.');
            });
            tiles.addTo(map);
            window.addEventListener('resize', function () { map.invalidateSize({animate: false, pan: false}); });
        },
        project: function (latitude, longitude) {
            return map.latLngToContainerPoint([latitude, longitude]);
        },
        reset: function () { map.fitBounds(bounds, {padding: [24, 24], animate: false}); }
    };
}());
