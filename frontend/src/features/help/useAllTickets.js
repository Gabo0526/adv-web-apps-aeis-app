import { useFetch } from '../../hooks/useFetch';
import { getAllTickets } from '../../api/helpApi';

export function useAllTickets() {
  return useFetch(getAllTickets, []);
}
