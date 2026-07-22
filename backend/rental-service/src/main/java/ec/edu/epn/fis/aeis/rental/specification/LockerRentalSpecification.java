package ec.edu.epn.fis.aeis.rental.specification;

import ec.edu.epn.fis.aeis.rental.dto.LockerRentalFilterDTO;
import ec.edu.epn.fis.aeis.rental.model.entity.LockerRental;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LockerRentalSpecification {

    private LockerRentalSpecification() {
    }

    public static Specification<LockerRental> build(LockerRentalFilterDTO filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filters.getUsername() != null) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + filters.getUsername().toLowerCase() + "%"));
            }

            if (filters.getBlockName() != null) {
                predicates.add(cb.equal(cb.lower(root.get("blockName")), filters.getBlockName().toLowerCase()));
            }

            if (filters.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filters.getStatus()));
            }

            if (filters.getStartDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), filters.getStartDateFrom()));
            }

            if (filters.getStartDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), filters.getStartDateTo()));
            }

            if (filters.getEndDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), filters.getEndDateFrom()));
            }

            if (filters.getEndDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), filters.getEndDateTo()));
            }

            if (filters.getPeriodName() != null) {
                predicates.add(cb.equal(cb.lower(root.get("period").get("name")), filters.getPeriodName().toLowerCase()));
            }

            if (filters.getRemainingDays() != null) {
                LocalDateTime cutoff = LocalDateTime.now().plusDays(filters.getRemainingDays());
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), cutoff));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
