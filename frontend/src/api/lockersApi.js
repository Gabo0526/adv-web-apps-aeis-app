import client from './client';

export function getLockerBlocks() {
  return client.get('/locker-blocks').then((res) => res.data);
}

export function getLockersByBlock(blockId) {
  return client.get(`/lockers/block/${blockId}`).then((res) => res.data);
}

export function createLockerBlock(payload) {
  return client.post('/locker-blocks', payload).then((res) => res.data);
}
