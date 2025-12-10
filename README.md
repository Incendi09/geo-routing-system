# Evac Route - Evacuation Routing System

A complete evacuation routing system with hazard (flood zone) avoidance. Built with Java 17 backend and React frontend.

![Hero](https://img.shields.io/badge/Java-17-blue) ![Gradle](https://img.shields.io/badge/Gradle-8.5-green) ![React](https://img.shields.io/badge/React-18.2-61DAFB)

## 🎯 Overview

This system computes safe evacuation routes that avoid or minimize travel through hazardous flood zones. It features:

- **Modified Dijkstra Algorithm** - Penalizes routes through flood zones rather than outright blocking them
- **GeoJSON Data Loading** - Loads road networks and flood polygons from standard GeoJSON files
- **Risk Scoring** - Calculates a risk score for each route based on hazard exposure
- **Interactive Map** - OpenLayers-based visualization with layer controls
- **Bilingual UI** - Full English and Polish translations

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          Frontend                                │
│  React + OpenLayers + react-hook-form + react-i18next           │
│  Port: 3000                                                      │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTP/JSON
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                          Backend                                 │
│  Java 17 + Jetty + RESTEasy + JTS                               │
│  Port: 8080                                                      │
├─────────────────────────────────────────────────────────────────┤
│  /api/evac/route?start=lat,lon&end=lat,lon                      │
│  /api/evac/health                                                │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Data Layer                               │
│  data/roads.geojson - Road network                               │
│  data/flood_zones.geojson - Hazard polygons                      │
└─────────────────────────────────────────────────────────────────┘
```

### Backend Structure

```
backend/src/main/java/com/sensorbite/evacroute/
├── Application.java          # Entry point
├── config/
│   ├── AppConfig.java        # JAX-RS application config
│   ├── JettyServer.java      # Embedded server setup
│   └── CorsFilter.java       # CORS for frontend
├── controller/
│   ├── EvacuationRouteController.java  # REST API endpoint
│   └── GlobalExceptionHandler.java     # Error handling
├── service/
│   └── RoutingService.java   # Orchestration layer
├── routing/
│   ├── graph/
│   │   ├── Graph.java        # Adjacency list graph
│   │   └── GraphBuilder.java # Builds graph with hazard info
│   └── algorithms/
│       ├── PathFinder.java   # Interface
│       └── DijkstraRouter.java  # Modified Dijkstra
├── geo/
│   ├── GeoJsonLoader.java    # Loads road network
│   ├── FloodZoneProvider.java # Interface
│   ├── MockFloodZoneProvider.java # Loads from GeoJSON
│   └── GeometryUtils.java    # JTS utilities
├── model/
│   ├── Coordinate.java       # Lat/lon value object
│   ├── Node.java             # Graph node
│   ├── Edge.java             # Graph edge with hazard cost
│   ├── RouteMetadata.java    # Route statistics
│   └── RouteResult.java      # GeoJSON response
└── exception/
    ├── InvalidInputException.java
    ├── NoRouteFoundException.java
    ├── DataLoadException.java
    └── ExternalServiceException.java
```

### Frontend Structure

```
frontend/src/
├── App.jsx                   # Main app with routing
├── index.js                  # Entry point
├── index.css                 # Global styles
├── api/
│   └── routing.js            # API client
├── components/
│   ├── Header.jsx            # App header
│   ├── LanguageSwitcher.jsx  # EN/PL toggle
│   ├── MapView.jsx           # OpenLayers map
│   ├── RouteForm.jsx         # Coordinate input form
│   └── RouteInfo.jsx         # Route metadata display
├── routes/
│   └── Home.jsx              # Main page
├── i18n/
│   ├── index.js              # i18next config
│   ├── en.json               # English translations
│   └── pl.json               # Polish translations
└── data/
    ├── roads.json            # Roads for map display
    └── floods.json           # Flood zones for map
```

## 🚀 Quick Start

### Prerequisites

- Java 17 JDK
- Node.js 18+ and npm
- Docker (optional)

### Running Locally

**Backend:**

```bash
cd backend

# On Windows
gradlew.bat run

# On Linux/Mac
./gradlew run
```

The backend will start on http://localhost:8080

**Frontend:**

```bash
cd frontend
npm install
npm start
```

The frontend will start on http://localhost:3000

### Running with Docker

```bash
docker-compose up --build
```

- Frontend: http://localhost:3000
- Backend: http://localhost:8080

## 📡 API Reference

### Calculate Route

```bash
GET /api/evac/route?start=lat,lon&end=lat,lon
```

**Example:**

```bash
curl "http://localhost:8080/api/evac/route?start=52.23,21.01&end=52.22,21.03"
```

**Response:**

```json
{
  "type": "Feature",
  "geometry": {
    "type": "LineString",
    "coordinates": [[21.01, 52.23], [21.012, 52.232], ...]
  },
  "properties": {
    "routeType": "evacuation",
    "distanceKm": "1.45"
  },
  "meta": {
    "totalDistanceMeters": 1450.5,
    "nodeCount": 12,
    "avoidedHazardSegments": 3,
    "hazardSegmentsTraversed": 0,
    "computationTimeMs": 15,
    "riskScore": 0.0
  }
}
```

### Health Check

```bash
GET /api/evac/health
```

**Response:**

```json
{
  "status": "ok",
  "graphNodes": 45,
  "graphEdges": 120,
  "hazardEdges": 8
}
```

### Error Responses

| Status | Description |
|--------|-------------|
| 400 | Invalid input (bad coordinates, missing params) |
| 404 | No route found between points |
| 500 | Server error (data load failure) |
| 502 | External service failure |

## 🔧 Configuration

The system is configured via environment variables with sensible defaults:

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Backend HTTP port |
| `ROADS_GEOJSON_PATH` | `data/roads.geojson` | Path to road network |
| `FLOODS_GEOJSON_PATH` | `data/flood_zones.geojson` | Path to flood zones |

## 🧪 Testing

```bash
cd backend
./gradlew test
```

Tests cover:
- Graph loading from GeoJSON
- Dijkstra routing without hazards
- Dijkstra routing with hazards (forcing detours)
- No route found scenarios
- Input validation

## 🎨 Creative Features

Beyond the basic requirements, this implementation includes:

1. **Risk Score** - A 0-1 score indicating how much of the route passes through hazards
2. **Hazard Avoidance Statistics** - Shows how many hazard segments were considered vs. used
3. **Interactive Map Layers** - Toggle visibility of roads, floods, and route independently
4. **Coordinate Snapping** - Automatically finds nearest road node to user coordinates
5. **Responsive Design** - Works on desktop and mobile devices
6. **Bilingual Support** - Full English and Polish translations

## 📁 Data Format

### Road Network (GeoJSON)

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "LineString",
        "coordinates": [[lon1, lat1], [lon2, lat2], ...]
      },
      "properties": {
        "name": "ul. Marszałkowska",
        "highway": "primary"
      }
    }
  ]
}
```

### Flood Zones (GeoJSON)

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "Polygon",
        "coordinates": [[[lon1, lat1], [lon2, lat2], ...]]
      },
      "properties": {
        "name": "Flood Zone A",
        "severity": "high"
      }
    }
  ]
}
```

## 🛠️ Tech Stack

### Backend
- **Java 17** - Modern Java with records, text blocks, enhanced switch
- **Gradle 8.5** - Build automation with pinned dependency versions
- **Jetty 11** - Embedded HTTP server
- **RESTEasy 6** - JAX-RS implementation
- **Jackson** - JSON serialization
- **JTS** - Java Topology Suite for geometry operations
- **SLF4J + Logback** - Logging
- **JUnit 5** - Testing

### Frontend
- **React 18** - UI framework
- **OpenLayers 8** - Interactive maps
- **React Router 6** - Client-side routing
- **React Hook Form** - Form handling
- **react-i18next** - Internationalization

### Infrastructure
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration
- **nginx** - Frontend static file serving and API proxy

---

## 🇵🇱 Podsumowanie po polsku

System trasowania ewakuacyjnego z unikaniem zagrożeń (stref zalewowych). Wykorzystuje zmodyfikowany algorytm Dijkstry, który penalizuje trasy przechodzące przez strefy niebezpieczne zamiast je blokować całkowicie.

**Główne funkcje:**
- Ładowanie sieci drogowej z plików GeoJSON
- Wykrywanie przecięć z poligonami zalewowymi
- Obliczanie bezpiecznych tras z metadanymi (dystans, czas, wskaźnik ryzyka)
- Interaktywna mapa z OpenLayers
- Pełna obsługa języka polskiego i angielskiego

**Uruchomienie:**
```bash
# Lokalnie
cd backend && ./gradlew run
cd frontend && npm install && npm start

# Lub z Dockerem
docker-compose up --build
```

---

## 📄 License

MIT License - feel free to use this code for any purpose.

## 👤 Author

Created as a recruitment task implementation demonstrating full-stack Java/React development skills.