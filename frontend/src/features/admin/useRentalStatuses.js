import { useFetch } from '../../hooks/useFetch';
import { getRentalStatuses } from '../../api/rentalsApi';

export function useRentalStatuses() {
  return useFetch(getRentalStatuses, []);
}
