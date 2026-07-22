import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { ROUTES } from '../routes';
import './Navbar.css';

const NAV_ITEMS = [
  { to: ROUTES.HOME, icon: 'fa-house', label: 'Inicio' },
  { to: ROUTES.LOCKERS, icon: 'fa-box-archive', label: 'Casilleros' },
  { to: ROUTES.MY_RENTALS, icon: 'fa-clipboard-list', label: 'Mis Alquileres' },
  { to: ROUTES.HELP, icon: 'fa-circle-question', label: 'Ayuda' },
];

const ADMIN_NAV_ITEMS = [
  { to: ROUTES.ADMIN_PERIODS, icon: 'fa-calendar-days', label: 'Períodos' },
  { to: ROUTES.ADMIN_BLOCKS_NEW, icon: 'fa-square-plus', label: 'Nuevo Bloque' },
  { to: ROUTES.ADMIN_RENTALS, icon: 'fa-file-invoice-dollar', label: 'Rentas' },
  { to: ROUTES.ADMIN_RENTALS_EXCEPTIONAL, icon: 'fa-user-clock', label: 'Renta Excepcional' },
  { to: ROUTES.ADMIN_USERS, icon: 'fa-users', label: 'Usuarios' },
];

export default function Navbar() {
  const { isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate(ROUTES.LOGIN);
  }

  const items = isAdmin ? [...NAV_ITEMS, ...ADMIN_NAV_ITEMS] : NAV_ITEMS;

  return (
    <nav className="navbar">
      <div className="navbar__inner">
        <Link to={ROUTES.HOME} className="navbar__brand">
          <i className="fa-solid fa-archive" /> AEIS
        </Link>

        <div className="navbar__links">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `navbar__link ${isActive ? 'navbar__link--active' : ''}`.trim()}
            >
              <i className={`fa-solid ${item.icon}`} />
              {item.label}
            </NavLink>
          ))}
        </div>

        <button type="button" className="navbar__logout" onClick={handleLogout}>
          <i className="fa-solid fa-right-from-bracket" /> Salir
        </button>
      </div>
    </nav>
  );
}
