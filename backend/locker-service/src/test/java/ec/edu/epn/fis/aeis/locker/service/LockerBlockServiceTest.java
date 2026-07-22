package ec.edu.epn.fis.aeis.locker.service;

import ec.edu.epn.fis.aeis.locker.dto.LockerBlockCreateRequestDTO;
import ec.edu.epn.fis.aeis.locker.dto.LockerBlockDTO;
import ec.edu.epn.fis.aeis.locker.exception.ResourceNotFoundException;
import ec.edu.epn.fis.aeis.locker.model.entity.Locker;
import ec.edu.epn.fis.aeis.locker.model.entity.LockerBlock;
import ec.edu.epn.fis.aeis.locker.repository.LockerBlockRepository;
import ec.edu.epn.fis.aeis.locker.repository.LockerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LockerBlockServiceTest {

    @Mock
    private LockerBlockRepository lockerBlockRepository;

    @Mock
    private LockerRepository lockerRepository;

    @InjectMocks
    private LockerBlockService lockerBlockService;

    private LockerBlock testLockerBlock1;
    private LockerBlock testLockerBlock2;

    @BeforeEach
    void setUp() {
        testLockerBlock1 = new LockerBlock();
        testLockerBlock1.setId(1L);
        testLockerBlock1.setName("Bloque A");
        testLockerBlock1.setBlockRows(5);
        testLockerBlock1.setBlockColumns(4);
        testLockerBlock1.setPeriodId(10L);
        testLockerBlock1.setAllowCustomRental(true);
        testLockerBlock1.setLockers(Collections.emptyList());

        testLockerBlock2 = new LockerBlock();
        testLockerBlock2.setId(2L);
        testLockerBlock2.setName("Bloque B");
        testLockerBlock2.setBlockRows(3);
        testLockerBlock2.setBlockColumns(6);
        testLockerBlock2.setPeriodId(10L);
        testLockerBlock2.setAllowCustomRental(false);
        testLockerBlock2.setLockers(Collections.emptyList());
    }

    @Test
    void givenExistingLockerBlocks_whenFindAllDTO_thenReturnsDTOList() {
        List<LockerBlock> lockerBlocks = Arrays.asList(testLockerBlock1, testLockerBlock2);
        when(lockerBlockRepository.findAllWithLockers()).thenReturn(lockerBlocks);

        List<LockerBlockDTO> result = lockerBlockService.findAllDTO();

        assertNotNull(result);
        assertEquals(2, result.size());

        LockerBlockDTO dto1 = result.get(0);
        assertEquals(testLockerBlock1.getId(), dto1.getId());
        assertEquals(testLockerBlock1.getName(), dto1.getName());
        assertEquals(testLockerBlock1.getBlockRows(), dto1.getBlockRows());
        assertEquals(testLockerBlock1.getBlockColumns(), dto1.getBlockColumns());
        assertEquals(testLockerBlock1.getPeriodId(), dto1.getPeriodId());
        assertEquals(testLockerBlock1.getAllowCustomRental(), dto1.getAllowCustomRental());
        assertTrue(dto1.getLockers().isEmpty());

        verify(lockerBlockRepository).findAllWithLockers();
    }

    @Test
    void givenNoLockerBlocks_whenFindAllDTO_thenReturnsEmptyDTOList() {
        when(lockerBlockRepository.findAllWithLockers()).thenReturn(Collections.emptyList());

        List<LockerBlockDTO> result = lockerBlockService.findAllDTO();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void givenValidId_whenFindById_thenReturnsBlock() {
        when(lockerBlockRepository.findById(1L)).thenReturn(Optional.of(testLockerBlock1));

        LockerBlock result = lockerBlockService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void givenInvalidId_whenFindById_thenThrowsResourceNotFoundException() {
        when(lockerBlockRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> lockerBlockService.findById(999L));
    }

    @Test
    void givenValidRequest_whenCreateWithLockers_thenCreatesBlockAndLockers() {
        LockerBlockCreateRequestDTO request = new LockerBlockCreateRequestDTO();
        request.setName("Bloque C");
        request.setBlockRows(2);
        request.setBlockColumns(3);
        request.setPeriodId(10L);
        request.setAllowCustomRental(true);
        request.setLockerLength(30.0);
        request.setLockerWidth(20.0);
        request.setLockerHeight(40.0);

        LockerBlock savedBlock = new LockerBlock();
        savedBlock.setId(5L);
        savedBlock.setName("Bloque C");
        savedBlock.setBlockRows(2);
        savedBlock.setBlockColumns(3);
        savedBlock.setPeriodId(10L);
        savedBlock.setAllowCustomRental(true);

        when(lockerBlockRepository.save(org.mockito.ArgumentMatchers.any(LockerBlock.class))).thenReturn(savedBlock);
        when(lockerRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        LockerBlock result = lockerBlockService.createWithLockers(request);

        assertNotNull(result);
        assertEquals(5L, result.getId());

        ArgumentCaptor<List<Locker>> captor = ArgumentCaptor.forClass(List.class);
        verify(lockerRepository).saveAll(captor.capture());
        List<Locker> lockers = captor.getValue();

        assertEquals(6, lockers.size());
        assertEquals(1, lockers.get(0).getNumber());
        assertEquals(6, lockers.get(5).getNumber());
        lockers.forEach(locker -> {
            assertEquals(30.0, locker.getLength());
            assertEquals(20.0, locker.getWidth());
            assertEquals(40.0, locker.getHeight());
            assertEquals(savedBlock, locker.getLockerBlock());
        });
    }
}
