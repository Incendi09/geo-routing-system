import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import './RouteForm.css';

// Regex to validate "lat, lon" format
const COORD_REGEX = /^-?\d+\.?\d*\s*,\s*-?\d+\.?\d*$/;

/**
 * Form for entering start and end coordinates.
 * Uses react-hook-form for validation and state management.
 */
function RouteForm({ onSubmit, isLoading }) {
    const { t } = useTranslation();

    const {
        register,
        handleSubmit,
        formState: { errors }
    } = useForm({
        defaultValues: {
            start: '52.23, 21.01',
            end: '52.22, 21.03'
        }
    });

    const validateCoord = (value) => {
        if (!COORD_REGEX.test(value)) {
            return t('form.errors.invalidFormat');
        }
        return true;
    };

    const onFormSubmit = (data) => {
        // Parse coordinates and pass to parent
        const parseCoord = (str) => {
            const [lat, lon] = str.split(',').map(s => parseFloat(s.trim()));
            return { lat, lon };
        };

        onSubmit({
            start: parseCoord(data.start),
            end: parseCoord(data.end)
        });
    };

    return (
        <form className="route-form card" onSubmit={handleSubmit(onFormSubmit)}>
            <h3 className="form-title">{t('form.title')}</h3>

            <div className="form-group">
                <label htmlFor="start">{t('form.start')}</label>
                <input
                    id="start"
                    type="text"
                    placeholder={t('form.startPlaceholder')}
                    {...register('start', {
                        required: t('form.errors.required'),
                        validate: validateCoord
                    })}
                />
                {errors.start && (
                    <span className="form-error">{errors.start.message}</span>
                )}
            </div>

            <div className="form-group">
                <label htmlFor="end">{t('form.end')}</label>
                <input
                    id="end"
                    type="text"
                    placeholder={t('form.endPlaceholder')}
                    {...register('end', {
                        required: t('form.errors.required'),
                        validate: validateCoord
                    })}
                />
                {errors.end && (
                    <span className="form-error">{errors.end.message}</span>
                )}
            </div>

            <button
                type="submit"
                className="btn btn-primary submit-btn"
                disabled={isLoading}
            >
                {isLoading ? (
                    <>
                        <span className="spinner"></span>
                        {t('form.calculating')}
                    </>
                ) : (
                    <>
                        {t('form.calculate')}
                    </>
                )}
            </button>
        </form>
    );
}

export default RouteForm;
