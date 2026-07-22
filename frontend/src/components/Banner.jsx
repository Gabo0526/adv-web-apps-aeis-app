import './Banner.css';

const ICONS = {
  success: 'fa-circle-check',
  error: 'fa-circle-exclamation',
};

export default function Banner({ type = 'error', children, onClose }) {
  if (!children) return null;

  return (
    <div className={`banner banner--${type}`}>
      <i className={`fa-solid ${ICONS[type]}`} />
      <span className="banner__message">{children}</span>
      {onClose && (
        <button type="button" className="banner__close" onClick={onClose} aria-label="Cerrar">
          <i className="fa-solid fa-xmark" />
        </button>
      )}
    </div>
  );
}
