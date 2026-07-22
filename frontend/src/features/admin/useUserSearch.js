import { useFetch } from '../../hooks/useFetch';
import { searchUsersByIdPrefix } from '../../api/authApi';

export function useUserSearch(idPrefix) {
  return useFetch(() => {
    const trimmed = idPrefix.trim();
    return trimmed.length >= 3 ? searchUsersByIdPrefix(trimmed) : Promise.resolve([]);
  }, [idPrefix]);
}
