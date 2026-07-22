import { useFetch } from '../../hooks/useFetch';
import { getLockerBlocks } from '../../api/lockersApi';

export function useLockerBlocks() {
  return useFetch(getLockerBlocks, []);
}
