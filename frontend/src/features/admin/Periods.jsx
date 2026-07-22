import { useState } from 'react';
import Navbar from '../../components/Navbar';
import PageHeader from '../../components/PageHeader';
import Card from '../../components/Card';
import DataTable from '../../components/DataTable';
import StatusChip from '../../components/StatusChip';
import Button from '../../components/Button';
import Modal from '../../components/Modal';
import Banner from '../../components/Banner';
import PeriodFormModal from './PeriodFormModal';
import { usePeriods } from './usePeriods';
import { activatePeriod } from '../../api/rentalsApi';
import { extractErrorMessage } from '../../api/client';
import './Periods.css';

const ACTIVE_STATUS_MAP = {
  true: { label: 'Activo', color: 'var(--success-color)' },
  false: { label: 'Inactivo', color: 'var(--text-muted)' },
};

export default function Periods() {
  const { data: periods, loading, error, reload } = usePeriods();
  const [formTarget, setFormTarget] = useState(undefined);
  const [activating, setActivating] = useState(null);
  const [activateError, setActivateError] = useState('');
  const [activateSubmitting, setActivateSubmitting] = useState(false);

  function handleSaved() {
    setFormTarget(undefined);
    reload();
  }

  async function handleConfirmActivate() {
    setActivateSubmitting(true);
    setActivateError('');
    try {
      await activatePeriod(activating.id);
      setActivating(null);
      reload();
    } catch (err) {
      setActivateError(extractErrorMessage(err));
    } finally {
      setActivateSubmitting(false);
    }
  }

  const columns = [
    { key: 'name', header: 'Nombre' },
    { key: 'startDate', header: 'Inicio', render: (row) => row.startDate?.slice(0, 10) ?? '—' },
    { key: 'endDate', header: 'Fin', render: (row) => row.endDate?.slice(0, 10) ?? '—' },
    {
      key: 'active',
      header: 'Estado',
      render: (row) => <StatusChip status={String(row.active)} statusMap={ACTIVE_STATUS_MAP} />,
    },
    {
      key: 'actions',
      header: 'Acciones',
      render: (row) => (
        <div className="periods__actions">
          <Button variant="secondary" onClick={() => setFormTarget(row)}>
            Editar
          </Button>
          <Button variant="primary" disabled={row.active} onClick={() => setActivating(row)}>
            Activar
          </Button>
        </div>
      ),
    },
  ];

  return (
    <>
      <Navbar />
      <div className="page-content">
        <PageHeader
          title="Períodos"
          subtitle="Gestiona los períodos de arriendo de casilleros"
          actions={<Button onClick={() => setFormTarget(null)}>+ Nuevo período</Button>}
        />
        <Card>
          <DataTable
            columns={columns}
            data={periods ?? []}
            loading={loading}
            error={error}
            emptyMessage="Aún no hay períodos creados."
          />
        </Card>
      </div>

      {formTarget !== undefined && (
        <PeriodFormModal
          key={formTarget ? formTarget.id : 'new'}
          period={formTarget}
          onClose={() => setFormTarget(undefined)}
          onSaved={handleSaved}
        />
      )}

      <Modal
        open={!!activating}
        title="Activar período"
        onClose={() => setActivating(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setActivating(null)}>
              Cancelar
            </Button>
            <Button onClick={handleConfirmActivate} loading={activateSubmitting}>
              Confirmar
            </Button>
          </>
        }
      >
        <Banner type="error">{activateError}</Banner>
        <p>
          ¿Activar el período <strong>{activating?.name}</strong>? Esto desactivará el período actualmente activo.
        </p>
      </Modal>
    </>
  );
}
