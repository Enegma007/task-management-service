package com.taskmanagement.repository;

import com.taskmanagement.entity.Task;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public final class TaskSpecification {

    private TaskSpecification() {
    }

    public static Specification<Task> withFilters(Boolean completed, String assignedTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (completed != null) {
                predicates.add(cb.equal(root.get("isCompleted"), completed));
            }
            if (assignedTo != null && !assignedTo.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("assignedTo")), assignedTo.trim().toLowerCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
