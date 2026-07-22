import { useCallback } from 'react';
import { useFetch } from '../../hooks/useFetch';
import { getTicketMessages } from '../../api/helpApi';

export function useTicketMessages(ticketId) {
  const fetchFn = useCallback(() => getTicketMessages(ticketId), [ticketId]);
  return useFetch(fetchFn, [ticketId]);
}
