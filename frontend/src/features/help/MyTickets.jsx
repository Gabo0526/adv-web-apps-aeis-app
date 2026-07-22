import { Link, useNavigate } from 'react-router-dom';
import Navbar from '../../components/Navbar';
import PageHeader from '../../components/PageHeader';
import Card from '../../components/Card';
import DataTable from '../../components/DataTable';
import StatusChip from '../../components/StatusChip';
import Button from '../../components/Button';
import { useMyTickets } from './useMyTickets';
import { TICKET_STATUS } from '../../utils/constants';
import { ROUTES, helpTicketPath } from '../../routes';
import './HelpTickets.css';

const COLUMNS = [
  { key: 'subject', header: 'Asunto' },
  { key: 'rentalRef', header: 'Casillero', render: (row) => row.rentalRef || '—' },
  { key: 'createdAt', header: 'Creado', render: (row) => row.createdAt?.slice(0, 10) ?? '—' },
  { key: 'status', header: 'Estado', render: (row) => <StatusChip status={row.status} statusMap={TICKET_STATUS} /> },
  {
    key: 'actions',
    header: '',
    render: (row) => (
      <Link to={helpTicketPath(row.id)} className="help-tickets__open">
        Abrir <i className="fa-solid fa-arrow-right" />
      </Link>
    ),
  },
];

export default function MyTickets() {
  const navigate = useNavigate();
  const { data: tickets, loading, error } = useMyTickets();

  return (
    <>
      <Navbar />
      <div className="page-content">
        <PageHeader
          title="Ayuda"
          subtitle="Tus tickets de soporte"
          actions={<Button onClick={() => navigate(ROUTES.HELP_NEW)}>+ Nuevo ticket</Button>}
        />
        <Card>
          <DataTable
            columns={COLUMNS}
            data={tickets ?? []}
            loading={loading}
            error={error}
            emptyMessage="Aún no has creado ningún ticket de ayuda."
          />
        </Card>
      </div>
    </>
  );
}
