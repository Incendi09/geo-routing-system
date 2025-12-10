import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import en from './en.json';
import pl from './pl.json';

// Detect browser language, default to English
const browserLang = navigator.language.split('-')[0];
const defaultLang = ['en', 'pl'].includes(browserLang) ? browserLang : 'en';

i18n
    .use(initReactI18next)
    .init({
        resources: {
            en: { translation: en },
            pl: { translation: pl }
        },
        lng: defaultLang,
        fallbackLng: 'en',
        interpolation: {
            escapeValue: false // React already escapes values
        }
    });

export default i18n;
