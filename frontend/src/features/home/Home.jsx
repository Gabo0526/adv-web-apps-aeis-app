import { Link } from 'react-router-dom';
import Navbar from '../../components/Navbar';
import PageHeader from '../../components/PageHeader';
import Card from '../../components/Card';
import { useAuth } from '../../auth/AuthContext';
import { ROUTES } from '../../routes';
import './Home.css';

const QUICK_LINKS = [
  { to: ROUTES.LOCKERS, icon: 'fa-box-archive', title: 'Casilleros', desc: 'Consulta la disponibilidad y renta un casillero.' },
  { to: ROUTES.MY_RENTALS, icon: 'fa-clipboard-list', title: 'Mis Alquileres', desc: 'Revisa el estado de tus casilleros rentados.' },
  { to: ROUTES.HELP, icon: 'fa-circle-question', title: 'Ayuda', desc: 'Chatea en tiempo real con soporte.' },
];

const ADMIN_LINKS = [
  { to: ROUTES.ADMIN_PERIODS, icon: 'fa-calendar-days', title: 'Períodos', desc: 'Gestiona los períodos de arriendo.' },
  { to: ROUTES.ADMIN_BLOCKS_NEW, icon: 'fa-square-plus', title: 'Nuevo Bloque', desc: 'Crea un nuevo bloque de casilleros.' },
  { to: ROUTES.ADMIN_RENTALS, icon: 'fa-file-invoice-dollar', title: 'Rentas', desc: 'Consulta, filtra y exporta las rentas.' },
  { to: ROUTES.ADMIN_RENTALS_EXCEPTIONAL, icon: 'fa-user-clock', title: 'Renta Excepcional', desc: 'Registra una renta manual para un usuario.' },
  { to: ROUTES.ADMIN_USERS, icon: 'fa-users', title: 'Usuarios', desc: 'Administra los usuarios del sistema.' },
];

export default function Home() {
  const { user, isAdmin } = useAuth();
  const links = isAdmin ? [...QUICK_LINKS, ...ADMIN_LINKS] : QUICK_LINKS;

  return (
    <>
      <Navbar />
      <div className="page-content">
        <PageHeader
          title={`Hola, ${user?.name ?? user?.username}`}
          subtitle="Bienvenido al sistema de casilleros AEIS-EPN"
        />
        <div className="home-grid">
          {links.map((link) => (
            <Link key={link.to} to={link.to} className="home-card">
              <Card className="home-card__inner">
                <i className={`fa-solid ${link.icon} home-card__icon`} />
                <h3 className="home-card__title">{link.title}</h3>
                <p className="home-card__desc">{link.desc}</p>
              </Card>
            </Link>
          ))}
        </div>
      </div>
    </>
  );
}
