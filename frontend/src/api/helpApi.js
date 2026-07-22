import client from './client';

export function createTicket(payload) {
  return client.post('/help/tickets', payload).then((res) => res.data);
}

export function getMyTickets() {
  return client.get('/help/tickets/mine').then((res) => res.data);
}

export function getAllTickets() {
  return client.get('/help/tickets').then((res) => res.data);
}

export function getTicketMessages(ticketId) {
  return client.get(`/help/tickets/${ticketId}/messages`).then((res) => res.data);
}

export function closeTicket(ticketId) {
  return client.put(`/help/tickets/${ticketId}/close`).then((res) => res.data);
}
