package com.exercise.delivery.model.entities;

import com.exercise.delivery.model.enums.DeliveryStatus;
import com.exercise.delivery.model.enums.UserType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "delivery_letter")
public class DeliveryLetter {

    @Id
    @Column(name = "id", updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "letter_id", nullable = false, unique = true)
    private UUID letterId;

    @Column(name = "getter_id", nullable = false)
    private UUID getterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "getter_type", nullable = false)
    private UserType getterType;

    @Enumerated(EnumType.STRING)
    @Column(name = "letter_status")
    private DeliveryStatus status;

    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "deliveryLetter",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<DeliveryLetterStatus> statuses;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        DeliveryLetter that = (DeliveryLetter) o;
        return letterId.equals(that.letterId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
