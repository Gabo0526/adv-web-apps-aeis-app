import { useFetch } from '../../hooks/useFetch';
import { getAdminRentals, getFilteredRentals } from '../../api/rentalsApi';

export const RENTALS_PAGE_SIZE = 10;

function hasActiveFilters(filters) {
  return Object.values(filters).some((value) => value !== '' && value !== null && value !== undefined);
}

export function useAdminRentals({ filters, page }) {
  const active = hasActiveFilters(filters);
  return useFetch(
    () =>
      active
        ? getFilteredRentals(filters, { page, size: RENTALS_PAGE_SIZE })
        : getAdminRentals({ page, size: RENTALS_PAGE_SIZE }),
    [page, JSON.stringify(filters)]
  );
}
