import Spinner from './Spinner';
import Banner from './Banner';
import './DataTable.css';

export default function DataTable({
  columns,
  data = [],
  loading = false,
  error = null,
  emptyMessage = 'No hay resultados.',
  page = 0,
  totalPages = 1,
  onPageChange,
}) {
  if (loading) return <Spinner label="Cargando datos..." />;
  if (error) return <Banner type="error">{error}</Banner>;

  return (
    <div className="data-table">
      <table className="data-table__table">
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key}>{col.header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.length === 0 ? (
            <tr>
              <td className="data-table__empty" colSpan={columns.length}>
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((row, rowIndex) => (
              <tr key={row.id ?? rowIndex}>
                {columns.map((col) => (
                  <td key={col.key}>{col.render ? col.render(row) : row[col.key]}</td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>

      {onPageChange && totalPages > 1 && (
        <div className="data-table__pagination">
          <button
            type="button"
            className="data-table__page-btn"
            disabled={page <= 0}
            onClick={() => onPageChange(page - 1)}
          >
            <i className="fa-solid fa-chevron-left" />
          </button>
          <span className="data-table__page-info">
            Página {page + 1} de {totalPages}
          </span>
          <button
            type="button"
            className="data-table__page-btn"
            disabled={page >= totalPages - 1}
            onClick={() => onPageChange(page + 1)}
          >
            <i className="fa-solid fa-chevron-right" />
          </button>
        </div>
      )}
    </div>
  );
}
