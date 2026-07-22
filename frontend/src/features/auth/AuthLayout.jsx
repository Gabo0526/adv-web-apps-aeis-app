import './AuthLayout.css';

export default function AuthLayout({ title, icon, subtitle, children, maxWidth = 450 }) {
  return (
    <div className="auth-layout">
      <div className="auth-layout__card" style={{ maxWidth }}>
        <div className="auth-layout__header">
          {icon && <i className={`fa-solid ${icon} auth-layout__icon`} />}
          <h1 className="auth-layout__title">{title}</h1>
          {subtitle && <p className="auth-layout__subtitle">{subtitle}</p>}
        </div>
        {children}
      </div>
    </div>
  );
}
