package com.exercise.delivery.repository;

import com.exercise.delivery.model.entities.DeliveryLetter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryLetterRepository extends JpaRepository<DeliveryLetter, Long> {

    @EntityGraph(attributePaths = {"statuses"})
    Optional<DeliveryLetter> findByLetterId(UUID id);

    @Query("""
          SELECT dl.letterId FROM DeliveryLetter dl
          WHERE dl.status IS NULL
    """)
    List<UUID> findAllLetterIdsByStatusIsNull(Pageable pageable);
}
