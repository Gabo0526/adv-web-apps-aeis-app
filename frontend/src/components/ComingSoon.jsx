import Navbar from './Navbar';
import PageHeader from './PageHeader';
import Card from './Card';

export default function ComingSoon({ title }) {
  return (
    <>
      <Navbar />
      <div className="page-content">
        <PageHeader title={title} subtitle="Esta sección estará disponible próximamente." />
        <Card>
          <p style={{ color: 'var(--text-muted)', margin: 0 }}>
            <i className="fa-solid fa-hammer" /> Estamos trabajando en esta funcionalidad.
          </p>
        </Card>
      </div>
    </>
  );
}
