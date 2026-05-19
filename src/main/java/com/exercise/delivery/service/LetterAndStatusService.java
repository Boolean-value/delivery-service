package com.exercise.delivery.service;

import com.exercise.delivery.model.entities.DeliveryLetter;
import com.exercise.delivery.model.entities.DeliveryLetterStatus;
import com.exercise.delivery.repository.DeliveryLetterRepository;
import com.exercise.delivery.repository.DeliveryLetterStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LetterAndStatusService {

    private final DeliveryLetterRepository letterRepository;
    private final DeliveryLetterStatusRepository statusRepository;

    @Transactional(readOnly = true)
    public List<UUID> getAllLetterIdsByNullStatus(Pageable pageable) {
        return letterRepository.findAllLetterIdsByStatusIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public DeliveryLetter getByLetterId(UUID letterId) {
        return letterRepository.findByLetterId(letterId).orElse(null);
    }

    @Transactional
    public void saveDeliveryLetter(DeliveryLetter deliveryLetter) {
        letterRepository.save(deliveryLetter);
    }

    @Transactional
    public void saveDeliveryLetterStatus(DeliveryLetterStatus deliveryStatus) {
        statusRepository.save(deliveryStatus);
    }
}
