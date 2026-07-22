import { useFetch } from '../../hooks/useFetch';
import { getPeriods } from '../../api/rentalsApi';

export function usePeriods() {
  return useFetch(getPeriods, []);
}
