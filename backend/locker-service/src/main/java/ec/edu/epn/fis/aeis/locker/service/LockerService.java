package ec.edu.epn.fis.aeis.locker.service;

import ec.edu.epn.fis.aeis.locker.dto.LockerUpdateRequestDTO;
import ec.edu.epn.fis.aeis.locker.exception.LockerConflictException;
import ec.edu.epn.fis.aeis.locker.exception.ResourceNotFoundException;
import ec.edu.epn.fis.aeis.locker.model.entity.Locker;
import ec.edu.epn.fis.aeis.locker.model.enums.LockerStatus;
import ec.edu.epn.fis.aeis.locker.repository.LockerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LockerService {

    private final LockerRepository lockerRepository;

    public LockerService(LockerRepository lockerRepository) {
        this.lockerRepository = lockerRepository;
    }

    public List<Locker> findByLockerBlockId(Long lockerBlockId) {
        return lockerRepository.findByLockerBlockId(lockerBlockId);
    }

    public Locker findById(Long lockerId) {
        return lockerRepository.findById(lockerId)
                .orElseThrow(() -> new ResourceNotFoundException("Casillero no encontrado con ID: " + lockerId));
    }

    public Locker findByIdForUpdate(Long lockerId) {
        return lockerRepository.findByIdForUpdate(lockerId)
                .orElseThrow(() -> new ResourceNotFoundException("Casillero no encontrado con ID: " + lockerId));
    }

    @Transactional
    public Locker updateDimensions(Long lockerId, LockerUpdateRequestDTO request) {
        Locker locker = findById(lockerId);

        if (locker.getStatus() != LockerStatus.AVAILABLE) {
            throw new LockerConflictException(
                    "Solo se pueden editar las dimensiones de un casillero disponible");
        }

        locker.setLength(request.getLength());
        locker.setWidth(request.getWidth());
        locker.setHeight(request.getHeight());

        return lockerRepository.save(locker);
    }

    @Transactional
    public Locker toggleMaintenanceStatus(Long lockerId) {
        Locker locker = findById(lockerId);

        if (locker.getStatus() == LockerStatus.AVAILABLE) {
            locker.setStatus(LockerStatus.UNDER_MAINTENANCE);
        } else if (locker.getStatus() == LockerStatus.UNDER_MAINTENANCE) {
            locker.setStatus(LockerStatus.AVAILABLE);
        } else {
            throw new LockerConflictException(
                    "Solo se puede alternar mantenimiento en un casillero disponible o en mantenimiento");
        }

        return lockerRepository.save(locker);
    }

    @Transactional
    public void deleteById(Long lockerId) {
        Locker locker = findById(lockerId);

        if (locker.getStatus() != LockerStatus.AVAILABLE) {
            throw new LockerConflictException("Solo se puede eliminar un casillero disponible");
        }

        lockerRepository.delete(locker);
    }

    @Transactional
    public Locker reserve(Long lockerId) {
        Locker locker = findByIdForUpdate(lockerId);

        if (locker.getStatus() != LockerStatus.AVAILABLE) {
            throw new LockerConflictException("El casillero no está disponible para reservar");
        }

        locker.setStatus(LockerStatus.PENDING);
        return lockerRepository.save(locker);
    }

    @Transactional
    public Locker occupy(Long lockerId) {
        Locker locker = findById(lockerId);

        if (locker.getStatus() != LockerStatus.PENDING) {
            throw new LockerConflictException("El casillero no está pendiente de asignación");
        }

        locker.setStatus(LockerStatus.OCCUPIED);
        return lockerRepository.save(locker);
    }

    @Transactional
    public Locker release(Long lockerId) {
        Locker locker = findById(lockerId);

        if (locker.getStatus() != LockerStatus.PENDING && locker.getStatus() != LockerStatus.OCCUPIED) {
            throw new LockerConflictException("El casillero no está pendiente ni ocupado");
        }

        locker.setStatus(LockerStatus.AVAILABLE);
        return lockerRepository.save(locker);
    }
}
