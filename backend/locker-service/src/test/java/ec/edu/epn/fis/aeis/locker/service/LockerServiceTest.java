package ec.edu.epn.fis.aeis.locker.service;

import ec.edu.epn.fis.aeis.locker.dto.LockerUpdateRequestDTO;
import ec.edu.epn.fis.aeis.locker.exception.LockerConflictException;
import ec.edu.epn.fis.aeis.locker.exception.ResourceNotFoundException;
import ec.edu.epn.fis.aeis.locker.model.entity.Locker;
import ec.edu.epn.fis.aeis.locker.model.entity.LockerBlock;
import ec.edu.epn.fis.aeis.locker.model.enums.LockerStatus;
import ec.edu.epn.fis.aeis.locker.repository.LockerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LockerServiceTest {

    @Mock
    private LockerRepository lockerRepository;

    @InjectMocks
    private LockerService lockerService;

    private Locker testLocker;
    private LockerBlock testLockerBlock;

    @BeforeEach
    void setUp() {
        testLockerBlock = new LockerBlock();
        testLockerBlock.setId(1L);
        testLockerBlock.setName("Bloque A");
        testLockerBlock.setAllowCustomRental(true);

        testLocker = new Locker();
        testLocker.setId(1L);
        testLocker.setNumber(101);
        testLocker.setStatus(LockerStatus.AVAILABLE);
        testLocker.setLength(30.0);
        testLocker.setWidth(20.0);
        testLocker.setHeight(40.0);
        testLocker.setLockerBlock(testLockerBlock);
    }

    @Test
    void givenValidLockerBlockId_whenFindByLockerBlockId_thenReturnsLockersList() {
        Locker locker2 = new Locker();
        locker2.setId(2L);
        locker2.setNumber(102);
        locker2.setLockerBlock(testLockerBlock);

        List<Locker> lockers = Arrays.asList(testLocker, locker2);
        when(lockerRepository.findByLockerBlockId(1L)).thenReturn(lockers);

        List<Locker> result = lockerService.findByLockerBlockId(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(lockerRepository).findByLockerBlockId(1L);
    }

    @Test
    void givenEmptyLockerBlock_whenFindByLockerBlockId_thenReturnsEmptyList() {
        when(lockerRepository.findByLockerBlockId(1L)).thenReturn(Collections.emptyList());

        List<Locker> result = lockerService.findByLockerBlockId(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void givenValidLockerId_whenFindById_thenReturnsLocker() {
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));

        Locker result = lockerService.findById(1L);

        assertNotNull(result);
        assertEquals(testLocker.getId(), result.getId());
    }

    @Test
    void givenInvalidLockerId_whenFindById_thenThrowsResourceNotFoundException() {
        when(lockerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> lockerService.findById(999L));
    }

    @Test
    void givenAvailableLocker_whenUpdateDimensions_thenUpdatesAndSaves() {
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));
        when(lockerRepository.save(testLocker)).thenReturn(testLocker);

        LockerUpdateRequestDTO request = new LockerUpdateRequestDTO();
        request.setLength(50.0);
        request.setWidth(25.0);
        request.setHeight(45.0);

        Locker result = lockerService.updateDimensions(1L, request);

        assertEquals(50.0, result.getLength());
        assertEquals(25.0, result.getWidth());
        assertEquals(45.0, result.getHeight());
        verify(lockerRepository).save(testLocker);
    }

    @Test
    void givenOccupiedLocker_whenUpdateDimensions_thenThrowsConflict() {
        testLocker.setStatus(LockerStatus.OCCUPIED);
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));

        LockerUpdateRequestDTO request = new LockerUpdateRequestDTO();
        request.setLength(50.0);
        request.setWidth(25.0);
        request.setHeight(45.0);

        assertThrows(LockerConflictException.class, () -> lockerService.updateDimensions(1L, request));
        verify(lockerRepository, never()).save(testLocker);
    }

    @Test
    void givenAvailableLocker_whenToggleMaintenanceStatus_thenSetsToMaintenance() {
        testLocker.setStatus(LockerStatus.AVAILABLE);
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));
        when(lockerRepository.save(testLocker)).thenReturn(testLocker);

        lockerService.toggleMaintenanceStatus(1L);

        assertEquals(LockerStatus.UNDER_MAINTENANCE, testLocker.getStatus());
        verify(lockerRepository).save(testLocker);
    }

    @Test
    void givenMaintenanceLocker_whenToggleMaintenanceStatus_thenSetsToAvailable() {
        testLocker.setStatus(LockerStatus.UNDER_MAINTENANCE);
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));
        when(lockerRepository.save(testLocker)).thenReturn(testLocker);

        lockerService.toggleMaintenanceStatus(1L);

        assertEquals(LockerStatus.AVAILABLE, testLocker.getStatus());
    }

    @Test
    void givenOccupiedLocker_whenToggleMaintenanceStatus_thenThrowsConflict() {
        testLocker.setStatus(LockerStatus.OCCUPIED);
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));

        assertThrows(LockerConflictException.class, () -> lockerService.toggleMaintenanceStatus(1L));
        verify(lockerRepository, never()).save(testLocker);
    }

    @Test
    void givenPendingLocker_whenToggleMaintenanceStatus_thenThrowsConflict() {
        testLocker.setStatus(LockerStatus.PENDING);
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));

        assertThrows(LockerConflictException.class, () -> lockerService.toggleMaintenanceStatus(1L));
    }

    @Test
    void givenAvailableLocker_whenDelete_thenDeletesLocker() {
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));

        lockerService.deleteById(1L);

        verify(lockerRepository).delete(testLocker);
    }

    @Test
    void givenNonAvailableLocker_whenDelete_thenThrowsConflict() {
        testLocker.setStatus(LockerStatus.OCCUPIED);
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));

        assertThrows(LockerConflictException.class, () -> lockerService.deleteById(1L));
        verify(lockerRepository, never()).delete(testLocker);
    }

    @Test
    void givenAvailableLocker_whenReserve_thenSetsToPending() {
        when(lockerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testLocker));
        when(lockerRepository.save(testLocker)).thenReturn(testLocker);

        Locker result = lockerService.reserve(1L);

        assertEquals(LockerStatus.PENDING, result.getStatus());
        verify(lockerRepository).save(testLocker);
    }

    @Test
    void givenNonAvailableLocker_whenReserve_thenThrowsConflict() {
        testLocker.setStatus(LockerStatus.PENDING);
        when(lockerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testLocker));

        assertThrows(LockerConflictException.class, () -> lockerService.reserve(1L));
        verify(lockerRepository, never()).save(testLocker);
    }

    @Test
    void givenPendingLocker_whenOccupy_thenSetsToOccupied() {
        testLocker.setStatus(LockerStatus.PENDING);
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));
        when(lockerRepository.save(testLocker)).thenReturn(testLocker);

        Locker result = lockerService.occupy(1L);

        assertEquals(LockerStatus.OCCUPIED, result.getStatus());
    }

    @Test
    void givenAvailableLocker_whenOccupy_thenThrowsConflict() {
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));

        assertThrows(LockerConflictException.class, () -> lockerService.occupy(1L));
        verify(lockerRepository, never()).save(testLocker);
    }

    @Test
    void givenPendingLocker_whenRelease_thenSetsToAvailable() {
        testLocker.setStatus(LockerStatus.PENDING);
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));
        when(lockerRepository.save(testLocker)).thenReturn(testLocker);

        Locker result = lockerService.release(1L);

        assertEquals(LockerStatus.AVAILABLE, result.getStatus());
    }

    @Test
    void givenOccupiedLocker_whenRelease_thenSetsToAvailable() {
        testLocker.setStatus(LockerStatus.OCCUPIED);
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));
        when(lockerRepository.save(testLocker)).thenReturn(testLocker);

        Locker result = lockerService.release(1L);

        assertEquals(LockerStatus.AVAILABLE, result.getStatus());
    }

    @Test
    void givenAvailableLocker_whenRelease_thenThrowsConflict() {
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(testLocker));

        assertThrows(LockerConflictException.class, () -> lockerService.release(1L));
        verify(lockerRepository, never()).save(testLocker);
    }
}
