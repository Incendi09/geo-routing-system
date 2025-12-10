import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import MapView from '../components/MapView';
import RouteForm from '../components/RouteForm';
import RouteInfo from '../components/RouteInfo';
import { calculateRoute } from '../api/routing';
import './Home.css';

/**
 * Home page with map and route planning interface.
 */
function Home() {
    const { t } = useTranslation();
    const [route, setRoute] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const [startPoint, setStartPoint] = useState(null);
    const [endPoint, setEndPoint] = useState(null);

    const handleCalculateRoute = async ({ start, end }) => {
        setIsLoading(true);
        setError(null);
        setStartPoint(start);
        setEndPoint(end);

        try {
            const result = await calculateRoute(start, end);
            setRoute(result);
        } catch (err) {
            console.error('Route calculation failed:', err);

            // Translate common error messages
            let errorMessage = err.message;
            if (err.message.includes('No route found')) {
                errorMessage = t('error.noRoute');
            } else if (err.message.includes('fetch')) {
                errorMessage = t('error.networkError');
            } else if (err.message.includes('500')) {
                errorMessage = t('error.serverError');
            }

            setError(errorMessage);
            setRoute(null);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="home">
            <div className="home-sidebar">
                <RouteForm onSubmit={handleCalculateRoute} isLoading={isLoading} />
                <RouteInfo route={route} error={error} />
            </div>

            <div className="home-map">
                <MapView
                    route={route}
                    startPoint={startPoint}
                    endPoint={endPoint}
                />
            </div>
        </div>
    );
}

export default Home;
