import { useFetch } from '../../hooks/useFetch';
import { getMyRentals } from '../../api/rentalsApi';

export function useMyRentals() {
  return useFetch(getMyRentals, []);
}
