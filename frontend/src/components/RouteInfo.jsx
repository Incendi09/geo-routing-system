import { useTranslation } from 'react-i18next';
import { IconDistance, IconHash, IconShield, IconAlert, IconRoute } from './Icons';
import './RouteInfo.css';

/**
 * Displays route metadata after calculation.
 * Shows distance, timing, risk score, and hazard info.
 */
function RouteInfo({ route, error }) {
    const { t } = useTranslation();

    if (error) {
        return (
            <div className="route-info card error">
                <h3 className="info-title error-title">
                    <IconAlert size={24} />
                    {t('error.title')}
                </h3>
                <p className="error-message">{error}</p>
            </div>
        );
    }

    if (!route) {
        return (
            <div className="route-info card empty">
                <h3 className="info-title">
                    <IconRoute size={24} />
                    {t('route.title')} V2
                </h3>
                <p className="empty-message">{t('route.noRoute')}</p>
            </div>
        );
    }

    const { meta } = route;
    const distanceKm = (meta.totalDistanceMeters / 1000).toFixed(2);

    // Determine risk level for styling
    const getRiskLevel = (score) => {
        if (score < 0.2) return { label: t('route.safe'), className: 'safe' };
        if (score < 0.5) return { label: t('route.moderate'), className: 'moderate' };
        return { label: t('route.risky'), className: 'risky' };
    };

    const riskLevel = getRiskLevel(meta.riskScore);

    return (
        <div className="route-info card animate-fade-in">
            <h3 className="info-title">
                <IconRoute size={24} />
                {t('route.title')}
            </h3>

            <div className="info-grid">
                <div className="info-item">
                    <span className="info-label">{t('route.distance')}</span>
                    <span className="info-value">{distanceKm} km</span>
                </div>

                <div className="info-item">
                    <span className="info-label">{t('route.nodes')}</span>
                    <span className="info-value">{meta.nodeCount}</span>
                </div>

                <div className="info-item">
                    <span className="info-label">{t('route.computeTime')}</span>
                    <span className="info-value">{meta.computationTimeMs} ms</span>
                </div>

                <div className="info-item">
                    <span className="info-label">{t('route.riskScore')}</span>
                    <span className={`info-value risk-badge ${riskLevel.className}`}>
                        {(meta.riskScore * 100).toFixed(0)}% - {riskLevel.label}
                    </span>
                </div>

                <div className="info-item">
                    <span className="info-label">{t('route.hazardAvoided')}</span>
                    <span className="info-value success">{meta.avoidedHazardSegments}</span>
                </div>

                <div className="info-item">
                    <span className="info-label">{t('route.hazardTraversed')}</span>
                    <span className="info-value warning">{meta.hazardSegmentsTraversed}</span>
                </div>
            </div>
        </div>
    );
}

export default RouteInfo;
