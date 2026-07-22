import './Button.css';

export default function Button({
  variant = 'primary',
  loading = false,
  type = 'button',
  className = '',
  disabled = false,
  children,
  ...rest
}) {
  return (
    <button
      type={type}
      className={`btn btn--${variant} ${className}`.trim()}
      disabled={disabled || loading}
      {...rest}
    >
      {loading && <i className="fa-solid fa-spinner fa-spin btn__spinner" />}
      {children}
    </button>
  );
}
