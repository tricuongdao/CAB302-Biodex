# Heat Map for Pests

## Document Control

| Field             | Value                                                                                          |
|-------------------|------------------------------------------------------------------------------------------------|
| Module            | Core Features → **Heat Map**                                                                   |
| Page Owner        | Joshua                                                                                         |
| Related Sub-pages | Report Sighting, Species Details                                                               |
| App               | Invasive Species Tracker (pest photo ID, location logging, ethical handling/disposal guidance) |
| Tech Stack        | JavaFX (UI/Controllers), WebView (Leaflet.js), SQL via JDBC, ALA API                           |
| Status            | Draft v1.0                                                                                     |

**Reference Document:** [Brisbane City Council: Invasive Species](https://www.brisbane.qld.gov.au/environment-and-water/wildlife-and-conservation/invasive-species)

## Overview
The main objective of this page is to track and catalogue invasive species and to provide awareness and information about how to ethically handle and/or remove them. This map interface must show an intuitive visual representation of pest density.

## Functional Requirements

* **Geographic display:** A clean and uncluttered map centred on the Brisbane area.
* **Data visualisation:** Areas with reported sightings highlighted using a gradient colour scale.
* **Filtering:** 
    * **By species:** Users can filter by a list of common local invasive species.
    * **By time:** Users can filter sightings by date.
* **Species details:** When a specific coloured hotspot or sighting on the map is clicked, the species details page for the selected species is shown.
* **Search functionality:** A search field allows users to search for a specific area (postcode, suburb, etc.) or specific species.
* **Reporting:** An easily visible primary call to action button labelled "Report Sighting".
* **Visual guide:** A small floating visual guide explaining what the colour zones mean in terms of pest density.

## User Stories

* *As a user*, I want to see a visual representation of pest density in Brisbane, so I know which areas to avoid or monitor closely.
* *As a user*, I want to filter the map by specific invasive species, so I can track the spread of a particular threat like Fire Ants.
* *As a user*, I want to distinguish between official biosecurity data and unverified user reports, so I can trust the accuracy of the map.
* *As a biosecurity officer (admin)*, I want to view clustered reports, so I can identify potential new outbreak zones efficiently.

## Technical Requirements

### Frontend
* Interactive map canvas (utilising a library such as Leaflet.js embedded via JavaFX WebView) locked to the Greater Brisbane region.
* Heat map data overlay processing to calculate visual density and apply colour gradients based on sighting volume.
* Form interface for users to report sightings, incorporating a dynamic search bar for species identification.
* Dynamic geospatial clustering to group close coordinate points into numbered nodes when the map is zoomed out.
* Contextual data pop-ups triggered by map clicks, displaying species identification and actionable handling links.

### Backend
* Java HTTP client integration to securely query `https://api.ala.org.au/species/search/auto` for autocomplete species search and identification.
* Asynchronous execution of all network calls and database queries using JavaFX `Task` or `Service` classes to ensure the main application thread does not freeze.
* Data sanitisation logic to validate incoming GPS coordinates, automatically rejecting data points that fall outside the predefined Greater Brisbane boundaries.
* JSON deserialisation (utilising libraries such as Gson or Jackson) to parse the nested text payload from the ALA API into usable Java objects.

### Database
* Local SQLite database implemented as the primary data store, connected via the `sqlite-jdbc` driver.
* Sighting record table: submission ID, user ID, species ID, date, latitude (REAL), longitude (REAL), and verification status.
* Species reference caching table to store results from the ALA API (scientific name, common name, API reference ID), establishing a local source of truth.
* Prepared statements for all SQL queries to ensure safe data retrieval and prevent SQL injection vulnerabilities.
