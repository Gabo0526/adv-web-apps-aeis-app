import { useState } from 'react';
import Navbar from '../../components/Navbar';
import PageHeader from '../../components/PageHeader';
import Card from '../../components/Card';
import DataTable from '../../components/DataTable';
import FormField from '../../components/FormField';
import { useUsers } from './useUsers';
import './Users.css';

const COLUMNS = [
  { key: 'id', header: 'Cédula' },
  { key: 'username', header: 'Usuario' },
  { key: 'name', header: 'Nombres' },
  { key: 'lastName', header: 'Apellidos' },
  { key: 'email', header: 'Email' },
  { key: 'college', header: 'Facultad' },
  { key: 'enabled', header: 'Estado' },
];

export default function Users() {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const { data, loading, error } = useUsers({ page, search });

  function handleSearchChange(e) {
    setSearch(e.target.value);
    setPage(0);
  }

  return (
    <>
      <Navbar />
      <div className="page-content">
        <PageHeader title="Usuarios" subtitle="Consulta los usuarios registrados en el sistema" />

        <Card className="users__search">
          <FormField
            label="Buscar por cédula (mínimo 3 dígitos)"
            name="search"
            value={search}
            onChange={handleSearchChange}
            placeholder="Ej. 1725"
          />
        </Card>

        <Card>
          <DataTable
            columns={COLUMNS}
            data={data?.content ?? []}
            loading={loading}
            error={error}
            emptyMessage="No se encontraron usuarios."
            page={data?.pageNumber ?? 0}
            totalPages={data?.totalPages ?? 1}
            onPageChange={setPage}
          />
        </Card>
      </div>
    </>
  );
}
