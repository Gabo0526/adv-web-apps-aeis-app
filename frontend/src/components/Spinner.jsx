import './Spinner.css';

export default function Spinner({ label = 'Cargando...', centered = true }) {
  return (
    <div className={`spinner ${centered ? 'spinner--centered' : ''}`.trim()}>
      <i className="fa-solid fa-spinner fa-spin" />
      {label && <span className="spinner__label">{label}</span>}
    </div>
  );
}
