/**
 * API client for evacuation routing backend.
 */

const API_BASE = process.env.REACT_APP_API_URL || '/api';

/**
 * Calculate an evacuation route between two points.
 * 
 * @param {Object} start - Start point { lat, lon }
 * @param {Object} end - End point { lat, lon }
 * @returns {Promise<Object>} Route result with geometry and metadata
 */
export async function calculateRoute(start, end) {
    const startStr = `${start.lat},${start.lon}`;
    const endStr = `${end.lat},${end.lon}`;

    const url = `${API_BASE}/evac/route?start=${startStr}&end=${endStr}`;

    const response = await fetch(url);

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`);
    }

    return response.json();
}

/**
 * Get health status of the routing service.
 * 
 * @returns {Promise<Object>} Health info
 */
export async function getHealth() {
    const response = await fetch(`${API_BASE}/evac/health`);

    if (!response.ok) {
        throw new Error('Health check failed');
    }

    return response.json();
}
