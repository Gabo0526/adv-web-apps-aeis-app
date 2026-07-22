import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../../components/Navbar';
import PageHeader from '../../components/PageHeader';
import Card from '../../components/Card';
import DataTable from '../../components/DataTable';
import StatusChip from '../../components/StatusChip';
import FormField from '../../components/FormField';
import Button from '../../components/Button';
import Banner from '../../components/Banner';
import { useAdminRentals } from './useAdminRentals';
import { useRentalStatuses } from './useRentalStatuses';
import { getAdminRentals, getFilteredRentals, generateExcel } from '../../api/rentalsApi';
import { extractErrorMessage } from '../../api/client';
import { downloadBlob } from '../../utils/download';
import { RENTAL_STATUS } from '../../utils/constants';
import { ROUTES } from '../../routes';
import './Rentals.css';

const EMPTY_FILTERS = { status: '', startDateFrom: '', startDateTo: '', username: '' };
const EXPORT_SIZE = 5000;

const COLUMNS = [
  { key: 'username', header: 'Usuario' },
  { key: 'userFullName', header: 'Nombre' },
  { key: 'blockName', header: 'Bloque' },
  { key: 'lockerNumber', header: 'Casillero' },
  { key: 'periodName', header: 'Período' },
  { key: 'startDate', header: 'Inicio', render: (row) => row.startDate?.slice(0, 10) ?? '—' },
  { key: 'endDate', header: 'Fin', render: (row) => row.endDate?.slice(0, 10) ?? '—' },
  { key: 'amountPaid', header: 'Monto', render: (row) => `$${Number(row.amountPaid).toFixed(2)}` },
  { key: 'status', header: 'Estado', render: (row) => <StatusChip status={row.status} statusMap={RENTAL_STATUS} /> },
];

function buildQueryFilters(filters) {
  const query = {};
  if (filters.status) query.status = filters.status;
  if (filters.username) query.username = filters.username;
  if (filters.startDateFrom) query.startDateFrom = `${filters.startDateFrom}T00:00:00`;
  if (filters.startDateTo) query.startDateTo = `${filters.startDateTo}T23:59:59`;
  return query;
}

function toExportRow(rental) {
  return {
    Usuario: rental.username,
    Nombre: rental.userFullName,
    Bloque: rental.blockName,
    Casillero: rental.lockerNumber,
    Periodo: rental.periodName,
    'Fecha inicio': rental.startDate?.slice(0, 10) ?? '',
    'Fecha fin': rental.endDate?.slice(0, 10) ?? '',
    'Dias restantes': rental.remainingDays,
    'Monto pagado': rental.amountPaid,
    Estado: rental.status,
  };
}

export default function Rentals() {
  const navigate = useNavigate();
  const { data: statuses } = useRentalStatuses();
  const [formFilters, setFormFilters] = useState(EMPTY_FILTERS);
  const [appliedFilters, setAppliedFilters] = useState({});
  const [page, setPage] = useState(0);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState('');

  const { data, loading, error } = useAdminRentals({ filters: appliedFilters, page });

  function handleFilterChange(e) {
    const { name, value } = e.target;
    setFormFilters((prev) => ({ ...prev, [name]: value }));
  }

  function handleApplyFilters(e) {
    e.preventDefault();
    setAppliedFilters(buildQueryFilters(formFilters));
    setPage(0);
  }

  function handleClearFilters() {
    setFormFilters(EMPTY_FILTERS);
    setAppliedFilters({});
    setPage(0);
  }

  async function handleExport() {
    setExporting(true);
    setExportError('');
    try {
      const hasFilters = Object.keys(appliedFilters).length > 0;
      const result = hasFilters
        ? await getFilteredRentals(appliedFilters, { page: 0, size: EXPORT_SIZE })
        : await getAdminRentals({ page: 0, size: EXPORT_SIZE });
      const rows = result.content.map(toExportRow);
      if (rows.length === 0) {
        setExportError('No hay rentas que coincidan con los filtros para exportar.');
        return;
      }
      const blob = await generateExcel(rows, 'rentas');
      downloadBlob(blob, 'rentas.xlsx');
    } catch (err) {
      setExportError(extractErrorMessage(err));
    } finally {
      setExporting(false);
    }
  }

  return (
    <>
      <Navbar />
      <div className="page-content">
        <PageHeader
          title="Rentas"
          subtitle="Consulta, filtra y exporta las rentas de casilleros"
          actions={
            <>
              <Button variant="secondary" onClick={() => navigate(ROUTES.ADMIN_RENTALS_EXCEPTIONAL)}>
                + Renta excepcional
              </Button>
              <Button onClick={handleExport} loading={exporting}>
                Exportar Excel
              </Button>
            </>
          }
        />

        <Card className="rentals__filters">
          <form onSubmit={handleApplyFilters} className="rentals__filters-grid" noValidate>
            <FormField
              label="Estado"
              name="status"
              as="select"
              value={formFilters.status}
              onChange={handleFilterChange}
            >
              <option value="">Todos</option>
              {(statuses ?? []).map((status) => (
                <option key={status} value={status}>
                  {RENTAL_STATUS[status]?.label ?? status}
                </option>
              ))}
            </FormField>
            <FormField
              label="Usuario"
              name="username"
              placeholder="Buscar por usuario..."
              value={formFilters.username}
              onChange={handleFilterChange}
            />
            <FormField
              label="Inicio desde"
              name="startDateFrom"
              type="date"
              value={formFilters.startDateFrom}
              onChange={handleFilterChange}
            />
            <FormField
              label="Inicio hasta"
              name="startDateTo"
              type="date"
              value={formFilters.startDateTo}
              onChange={handleFilterChange}
            />
            <div className="rentals__filters-actions">
              <Button type="submit">Aplicar filtros</Button>
              <Button type="button" variant="secondary" onClick={handleClearFilters}>
                Limpiar
              </Button>
            </div>
          </form>
        </Card>

        <Banner type="error">{exportError}</Banner>

        <Card>
          <DataTable
            columns={COLUMNS}
            data={data?.content ?? []}
            loading={loading}
            error={error}
            emptyMessage="No hay rentas que coincidan con los filtros."
            page={data?.pageNumber ?? 0}
            totalPages={data?.totalPages ?? 1}
            onPageChange={setPage}
          />
        </Card>
      </div>
    </>
  );
}
