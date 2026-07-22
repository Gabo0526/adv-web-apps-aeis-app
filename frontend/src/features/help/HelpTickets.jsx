import { useAuth } from '../../auth/AuthContext';
import MyTickets from './MyTickets';
import AdminTickets from './AdminTickets';

export default function HelpTickets() {
  const { isAdmin } = useAuth();
  return isAdmin ? <AdminTickets /> : <MyTickets />;
}
