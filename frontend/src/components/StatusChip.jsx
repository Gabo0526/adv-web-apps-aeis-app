import './StatusChip.css';

export default function StatusChip({ status, statusMap }) {
  const entry = statusMap[status] ?? { label: status, color: 'var(--text-muted)' };

  return (
    <span className="status-chip" style={{ color: entry.color, borderColor: entry.color }}>
      {entry.label}
    </span>
  );
}
