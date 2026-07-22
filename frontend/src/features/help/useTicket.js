import { useAuth } from '../../auth/AuthContext';
import { useFetch } from '../../hooks/useFetch';
import { getAllTickets, getMyTickets } from '../../api/helpApi';

/**
 * No existe un GET /help/tickets/{id} individual (ver PLAN.md §7.2): se
 * reutiliza la lista de tickets del usuario (o de todos, si es admin) y se
 * busca el ticket por id. Sigue funcionando después de recargar la página.
 */
export function useTicket(ticketId) {
  const { isAdmin } = useAuth();
  const fetchFn = isAdmin ? getAllTickets : getMyTickets;
  const { data: tickets, loading, error, reload } = useFetch(fetchFn, [isAdmin]);
  const ticket = tickets?.find((t) => t.id === ticketId) ?? null;
  return { ticket, loading, error, reload };
}
