import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import OSM from 'ol/source/OSM';
import { fromLonLat } from 'ol/proj';
import { Style, Stroke, Fill, Icon } from 'ol/style';
import { LineString, Point, Polygon } from 'ol/geom';
import Feature from 'ol/Feature';
import 'ol/ol.css';
import './MapView.css';

// Import sample data - in production these would come from an API
import roadsData from '../data/roads.json';
import floodsData from '../data/floods.json';

// Import assets
import startMarkerIcon from '../assets/start_marker.png';
import endMarkerIcon from '../assets/end_marker.png';
import { IconRoad, IconWaves, IconRoute, IconLayers } from './Icons';

/**
 * OpenLayers map component.
 * Displays roads, flood zones, and the calculated route.
 */
function MapView({ route, startPoint, endPoint }) {
    const { t } = useTranslation();
    const mapRef = useRef(null);
    const mapInstanceRef = useRef(null);
    const routeLayerRef = useRef(null);
    const markersLayerRef = useRef(null);

    // Layers visibility state
    const [layersVisible, setLayersVisible] = useState({
        roads: true,
        floods: true,
        route: true
    });

    useEffect(() => {
        if (!mapRef.current || mapInstanceRef.current) return;

        // Roads layer - blue lines
        const roadsSource = new VectorSource();
        const roadsLayer = new VectorLayer({
            source: roadsSource,
            style: new Style({
                stroke: new Stroke({
                    color: 'rgba(59, 130, 246, 0.6)',
                    width: 2
                })
            })
        });

        // Add road features
        if (roadsData?.features) {
            roadsData.features.forEach(feature => {
                if (feature.geometry?.type === 'LineString') {
                    const coords = feature.geometry.coordinates.map(c => fromLonLat(c));
                    const line = new Feature({
                        geometry: new LineString(coords),
                        name: feature.properties?.name
                    });
                    roadsSource.addFeature(line);
                }
            });
        }

        // Flood zones layer - red polygons
        const floodsSource = new VectorSource();
        const floodsLayer = new VectorLayer({
            source: floodsSource,
            style: new Style({
                stroke: new Stroke({
                    color: 'rgba(239, 68, 68, 0.8)',
                    width: 2
                }),
                fill: new Fill({
                    color: 'rgba(239, 68, 68, 0.25)'
                })
            })
        });

        // Add flood zone features
        if (floodsData?.features) {
            floodsData.features.forEach(feature => {
                if (feature.geometry?.type === 'Polygon') {
                    const coords = feature.geometry.coordinates[0].map(c => fromLonLat(c));
                    const polygon = new Feature({
                        geometry: new Polygon([coords]),
                        name: feature.properties?.name
                    });
                    floodsSource.addFeature(polygon);
                }
            });
        }

        // Route layer - will be updated when route is calculated
        const routeSource = new VectorSource();
        const routeLayer = new VectorLayer({
            source: routeSource,
            style: new Style({
                stroke: new Stroke({
                    color: '#22c55e',
                    width: 4,
                    lineDash: [10, 5]
                })
            })
        });
        routeLayerRef.current = routeLayer;

        // Markers layer for start/end points
        const markersSource = new VectorSource();
        const markersLayer = new VectorLayer({
            source: markersSource,
            style: (feature) => {
                const type = feature.get('type');
                return new Style({
                    image: new Icon({
                        anchor: [0.5, 1],
                        scale: 0.05,
                        src: type === 'start' ? startMarkerIcon : endMarkerIcon
                    })
                });
            }
        });
        markersLayerRef.current = markersLayer;

        // Create map
        const map = new Map({
            target: mapRef.current,
            layers: [
                new TileLayer({
                    source: new OSM()
                }),
                roadsLayer,
                floodsLayer,
                routeLayer,
                markersLayer
            ],
            view: new View({
                center: fromLonLat([21.015, 52.23]),
                zoom: 15
            })
        });

        mapInstanceRef.current = map;

        // Store layer references for visibility toggling
        map.roadsLayer = roadsLayer;
        map.floodsLayer = floodsLayer;

        return () => {
            map.setTarget(null);
            mapInstanceRef.current = null;
        };
    }, []);

    // Update route on map when it changes
    useEffect(() => {
        if (!routeLayerRef.current || !route) return;

        const source = routeLayerRef.current.getSource();
        source.clear();

        // Add route line
        if (route.geometry?.coordinates) {
            const coords = route.geometry.coordinates.map(c => fromLonLat(c));
            const routeFeature = new Feature({
                geometry: new LineString(coords)
            });
            source.addFeature(routeFeature);

            // Fit view to route
            if (mapInstanceRef.current) {
                mapInstanceRef.current.getView().fit(routeFeature.getGeometry(), {
                    padding: [50, 50, 50, 50],
                    duration: 500
                });
            }
        }
    }, [route]);

    // Update markers when start/end points change
    useEffect(() => {
        if (!markersLayerRef.current) return;

        const source = markersLayerRef.current.getSource();
        source.clear();

        if (startPoint) {
            const startMarker = new Feature({
                geometry: new Point(fromLonLat([startPoint.lon, startPoint.lat])),
                type: 'start'
            });
            source.addFeature(startMarker);
        }

        if (endPoint) {
            const endMarker = new Feature({
                geometry: new Point(fromLonLat([endPoint.lon, endPoint.lat])),
                type: 'end'
            });
            source.addFeature(endMarker);
        }
    }, [startPoint, endPoint]);

    // Toggle layer visibility
    const toggleLayer = (layer) => {
        const map = mapInstanceRef.current;
        if (!map) return;

        const newVisibility = !layersVisible[layer];
        setLayersVisible(prev => ({ ...prev, [layer]: newVisibility }));

        if (layer === 'roads') map.roadsLayer.setVisible(newVisibility);
        if (layer === 'floods') map.floodsLayer.setVisible(newVisibility);
        if (layer === 'route') routeLayerRef.current?.setVisible(newVisibility);
    };

    return (
        <div className="map-container">
            <div ref={mapRef} className="map"></div>

            <div className="map-controls glass">
                <span className="controls-label">{t('map.layers.roads')}</span>
                <div className="layer-toggles">
                    <button
                        className={`layer-toggle ${layersVisible.roads ? 'active' : ''}`}
                        onClick={() => toggleLayer('roads')}
                        title={t('map.layers.roads')}
                    >
                        <IconRoad />
                    </button>
                    <button
                        className={`layer-toggle ${layersVisible.floods ? 'active' : ''}`}
                        onClick={() => toggleLayer('floods')}
                        title={t('map.layers.floods')}
                    >
                        <IconWaves />
                    </button>
                    <button
                        className={`layer-toggle ${layersVisible.route ? 'active' : ''}`}
                        onClick={() => toggleLayer('route')}
                        title={t('map.layers.route')}
                    >
                        <IconRoute />
                    </button>
                </div>
            </div>

            <div className="map-legend glass">
                <div className="legend-item">
                    <span className="legend-color" style={{ background: 'rgba(59, 130, 246, 0.6)' }}></span>
                    <span>{t('map.layers.roads')}</span>
                </div>
                <div className="legend-item">
                    <span className="legend-color" style={{ background: 'rgba(239, 68, 68, 0.5)' }}></span>
                    <span>{t('map.layers.floods')}</span>
                </div>
                <div className="legend-item">
                    <span className="legend-color" style={{ background: '#22c55e' }}></span>
                    <span>{t('map.layers.route')}</span>
                </div>
            </div>
        </div>
    );
}

export default MapView;
