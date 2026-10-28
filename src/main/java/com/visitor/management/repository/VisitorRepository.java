package com.visitor.management.repository;

import com.visitor.management.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;

public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    List<Visitor> findByNameContainingIgnoreCase(String name);

    List<Visitor> findByMobileContaining(String mobile);

    List<Visitor> findByCheckInTimeBetween(
        LocalDateTime start,
        LocalDateTime end
);
}