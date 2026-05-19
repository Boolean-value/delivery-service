package com.exercise.delivery.repository;

import com.exercise.delivery.model.entities.DeliveryLetterStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeliveryLetterStatusRepository extends JpaRepository<DeliveryLetterStatus, UUID> {
}
