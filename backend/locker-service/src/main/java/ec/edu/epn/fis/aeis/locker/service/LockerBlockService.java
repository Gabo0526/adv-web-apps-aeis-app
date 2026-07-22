package ec.edu.epn.fis.aeis.locker.service;

import ec.edu.epn.fis.aeis.locker.dto.LockerBlockCreateRequestDTO;
import ec.edu.epn.fis.aeis.locker.dto.LockerBlockDTO;
import ec.edu.epn.fis.aeis.locker.exception.ResourceNotFoundException;
import ec.edu.epn.fis.aeis.locker.model.entity.Locker;
import ec.edu.epn.fis.aeis.locker.model.entity.LockerBlock;
import ec.edu.epn.fis.aeis.locker.repository.LockerBlockRepository;
import ec.edu.epn.fis.aeis.locker.repository.LockerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LockerBlockService {

    private final LockerBlockRepository lockerBlockRepository;
    private final LockerRepository lockerRepository;

    public LockerBlockService(LockerBlockRepository lockerBlockRepository, LockerRepository lockerRepository) {
        this.lockerBlockRepository = lockerBlockRepository;
        this.lockerRepository = lockerRepository;
    }

    @Transactional
    public List<LockerBlockDTO> findAllDTO() {
        return lockerBlockRepository.findAllWithLockers()
                .stream()
                .map(LockerBlockDTO::new)
                .toList();
    }

    public LockerBlock findById(Long id) {
        return lockerBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bloque de casilleros no encontrado con ID: " + id));
    }

    @Transactional
    public LockerBlock createWithLockers(LockerBlockCreateRequestDTO request) {
        LockerBlock lockerBlock = new LockerBlock();
        lockerBlock.setName(request.getName());
        lockerBlock.setBlockRows(request.getBlockRows());
        lockerBlock.setBlockColumns(request.getBlockColumns());
        lockerBlock.setPeriodId(request.getPeriodId());
        lockerBlock.setAllowCustomRental(request.getAllowCustomRental());

        lockerBlock = lockerBlockRepository.save(lockerBlock);

        int totalLockers = request.getBlockRows() * request.getBlockColumns();
        List<Locker> lockers = new ArrayList<>();

        for (int i = 1; i <= totalLockers; i++) {
            Locker locker = new Locker();
            locker.setNumber(i);
            locker.setLength(request.getLockerLength());
            locker.setWidth(request.getLockerWidth());
            locker.setHeight(request.getLockerHeight());
            locker.setLockerBlock(lockerBlock);
            lockers.add(locker);
        }

        lockerBlock.setLockers(lockerRepository.saveAll(lockers));
        return lockerBlock;
    }
}
