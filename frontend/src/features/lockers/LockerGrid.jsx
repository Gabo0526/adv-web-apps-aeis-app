import { useState } from 'react';
import Navbar from '../../components/Navbar';
import PageHeader from '../../components/PageHeader';
import Card from '../../components/Card';
import Spinner from '../../components/Spinner';
import Banner from '../../components/Banner';
import RentModal from './RentModal';
import { useLockerBlocks } from './useLockerBlocks';
import { LOCKER_STATUS } from '../../utils/constants';
import './LockerGrid.css';

export default function LockerGrid() {
  const { data: blocks, loading, error, reload } = useLockerBlocks();
  const [selectedLocker, setSelectedLocker] = useState(null);

  function handleCellClick(locker, block) {
    if (locker.status !== 'AVAILABLE') return;
    setSelectedLocker({ ...locker, blockName: block.name, allowCustomRental: block.allowCustomRental });
  }

  function handleRented() {
    setSelectedLocker(null);
    reload();
  }

  return (
    <>
      <Navbar />
      <div className="page-content">
        <PageHeader title="Casilleros" subtitle="Elige un casillero disponible para rentarlo" />

        <div className="locker-legend">
          {Object.entries(LOCKER_STATUS).map(([key, { label, color }]) => (
            <span key={key} className="locker-legend__item">
              <span className="locker-legend__swatch" style={{ background: color }} />
              {label}
            </span>
          ))}
        </div>

        {loading && <Spinner label="Cargando casilleros..." />}
        {error && <Banner type="error">{error}</Banner>}

        {!loading && !error && blocks?.length === 0 && (
          <Banner type="error">Aún no hay bloques de casilleros creados.</Banner>
        )}

        {!loading &&
          !error &&
          blocks?.map((block) => (
            <Card key={block.id} className="locker-block">
              <h3 className="locker-block__title">{block.name}</h3>
              <div
                className="locker-block__grid"
                style={{ gridTemplateColumns: `repeat(${block.blockColumns}, 1fr)` }}
              >
                {block.lockers.map((locker) => (
                  <button
                    key={locker.id}
                    type="button"
                    className={`locker-cell locker-cell--${locker.status.toLowerCase()}`}
                    style={{ background: LOCKER_STATUS[locker.status]?.color }}
                    disabled={locker.status !== 'AVAILABLE'}
                    onClick={() => handleCellClick(locker, block)}
                    title={`Casillero ${locker.number} — ${LOCKER_STATUS[locker.status]?.label}`}
                  >
                    {locker.number}
                  </button>
                ))}
              </div>
            </Card>
          ))}
      </div>

      <RentModal locker={selectedLocker} onClose={() => setSelectedLocker(null)} onRented={handleRented} />
    </>
  );
}
