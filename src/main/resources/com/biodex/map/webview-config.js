// JavaFX WebView can mispaint transformed tile layers after zooming.
// Use Leaflet's 2D positioning path; this must run before Leaflet is loaded.
window.L_DISABLE_3D = true;
