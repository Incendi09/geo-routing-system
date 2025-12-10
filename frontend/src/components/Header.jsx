import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import LanguageSwitcher from './LanguageSwitcher';
import { IconShield } from './Icons';
import './Header.css';

/**
 * App header with logo and navigation.
 */
function Header() {
    const { t } = useTranslation();

    return (
        <header className="header">
            <div className="header-container">
                <Link to="/" className="header-logo">
                    <IconShield size={32} className="logo-icon-svg" />
                    <div className="logo-text">
                        <span className="logo-title">{t('app.title')}</span>
                        <span className="logo-subtitle">{t('app.subtitle')}</span>
                    </div>
                </Link>

                <nav className="header-nav">
                    <Link to="/" className="nav-link">{t('nav.home')}</Link>
                    <LanguageSwitcher />
                </nav>
            </div>
        </header>
    );
}

export default Header;
