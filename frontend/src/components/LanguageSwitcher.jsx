import { useTranslation } from 'react-i18next';
import { IconGlobe } from './Icons';
import './LanguageSwitcher.css';

/**
 * Language switcher component.
 * Toggles between English and Polish.
 */
function LanguageSwitcher() {
    const { i18n, t } = useTranslation();

    const toggleLanguage = () => {
        const newLang = i18n.language === 'en' ? 'pl' : 'en';
        i18n.changeLanguage(newLang);
    };

    return (
        <button
            className="lang-switcher"
            onClick={toggleLanguage}
            aria-label={t('language.switch')}
        >
            <IconGlobe size={18} />
            <span className="lang-code">
                {i18n.language === 'en' ? 'EN' : 'PL'}
            </span>
        </button>
    );
}

export default LanguageSwitcher;
