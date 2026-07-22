import { useFetch } from '../../hooks/useFetch';
import { getUsers, searchUsersByIdPrefix } from '../../api/authApi';

export const USERS_PAGE_SIZE = 10;

export function useUsers({ page, search }) {
  const searching = search.trim().length >= 3;
  return useFetch(async () => {
    if (searching) {
      const content = await searchUsersByIdPrefix(search.trim());
      return { content, pageNumber: 0, totalPages: 1, totalElements: content.length };
    }
    return getUsers({ page, size: USERS_PAGE_SIZE });
  }, [page, search]);
}
