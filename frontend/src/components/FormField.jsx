import './FormField.css';

export default function FormField({
  label,
  name,
  type = 'text',
  as = 'input',
  error,
  touched,
  className = '',
  children,
  ...rest
}) {
  const showError = touched && error;

  return (
    <div className={`form-field ${className}`.trim()}>
      {label && (
        <label className="form-field__label" htmlFor={name}>
          {label}
        </label>
      )}
      {as === 'select' ? (
        <select id={name} name={name} className="form-field__input" {...rest}>
          {children}
        </select>
      ) : as === 'textarea' ? (
        <textarea id={name} name={name} className="form-field__input" {...rest} />
      ) : (
        <input id={name} name={name} type={type} className="form-field__input" {...rest} />
      )}
      {showError && <span className="form-field__error">{error}</span>}
    </div>
  );
}
