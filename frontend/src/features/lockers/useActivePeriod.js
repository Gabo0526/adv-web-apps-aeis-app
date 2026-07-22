import { useFetch } from '../../hooks/useFetch';
import { getActivePeriod } from '../../api/rentalsApi';

export function useActivePeriod() {
  return useFetch(getActivePeriod, []);
}
