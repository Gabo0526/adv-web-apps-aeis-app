import Navbar from '../../components/Navbar';
import PageHeader from '../../components/PageHeader';
import Card from '../../components/Card';
import DataTable from '../../components/DataTable';
import StatusChip from '../../components/StatusChip';
import { useMyRentals } from './useMyRentals';
import { RENTAL_STATUS } from '../../utils/constants';

const COLUMNS = [
  { key: 'blockName', header: 'Bloque' },
  { key: 'lockerNumber', header: 'Casillero' },
  { key: 'periodName', header: 'Período' },
  { key: 'startDate', header: 'Inicio', render: (row) => row.startDate?.slice(0, 10) ?? '—' },
  { key: 'endDate', header: 'Fin', render: (row) => row.endDate?.slice(0, 10) ?? '—' },
  { key: 'remainingDays', header: 'Días restantes', render: (row) => (row.remainingDays >= 0 ? row.remainingDays : '—') },
  { key: 'amountPaid', header: 'Monto pagado', render: (row) => `$${Number(row.amountPaid).toFixed(2)}` },
  { key: 'status', header: 'Estado', render: (row) => <StatusChip status={row.status} statusMap={RENTAL_STATUS} /> },
];

export default function MyRentals() {
  const { data: rentals, loading, error } = useMyRentals();

  return (
    <>
      <Navbar />
      <div className="page-content">
        <PageHeader title="Mis Alquileres" subtitle="Historial de casilleros que has rentado" />
        <Card>
          <DataTable
            columns={COLUMNS}
            data={rentals ?? []}
            loading={loading}
            error={error}
            emptyMessage="Aún no has rentado ningún casillero."
          />
        </Card>
      </div>
    </>
  );
}
