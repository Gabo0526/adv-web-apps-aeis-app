import { useFetch } from '../../hooks/useFetch';
import { getMyTickets } from '../../api/helpApi';

export function useMyTickets() {
  return useFetch(getMyTickets, []);
}
