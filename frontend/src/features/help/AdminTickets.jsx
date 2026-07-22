import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../../components/Navbar';
import PageHeader from '../../components/PageHeader';
import Card from '../../components/Card';
import DataTable from '../../components/DataTable';
import StatusChip from '../../components/StatusChip';
import { useAllTickets } from './useAllTickets';
import { TICKET_STATUS } from '../../utils/constants';
import { helpTicketPath } from '../../routes';
import './HelpTickets.css';

const FILTERS = [
  { value: 'OPEN', label: 'Abiertos' },
  { value: 'CLOSED', label: 'Cerrados' },
  { value: 'ALL', label: 'Todos' },
];

const COLUMNS = [
  { key: 'username', header: 'Usuario' },
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

export default function AdminTickets() {
  const { data: tickets, loading, error } = useAllTickets();
  const [filter, setFilter] = useState('OPEN');

  const filtered = useMemo(() => {
    if (!tickets) return [];
    if (filter === 'ALL') return tickets;
    return tickets.filter((t) => t.status === filter);
  }, [tickets, filter]);

  return (
    <>
      <Navbar />
      <div className="page-content">
        <PageHeader title="Ayuda" subtitle="Tickets de soporte de todos los usuarios" />

        <div className="admin-tickets__filters">
          {FILTERS.map((f) => (
            <button
              key={f.value}
              type="button"
              className={`admin-tickets__filter ${filter === f.value ? 'admin-tickets__filter--active' : ''}`.trim()}
              onClick={() => setFilter(f.value)}
            >
              {f.label}
            </button>
          ))}
        </div>

        <Card>
          <DataTable
            columns={COLUMNS}
            data={filtered}
            loading={loading}
            error={error}
            emptyMessage="No hay tickets que coincidan con el filtro."
          />
        </Card>
      </div>
    </>
  );
}
