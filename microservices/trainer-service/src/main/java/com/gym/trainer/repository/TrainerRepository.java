
package com.gym.trainer.repository;

import com.gym.trainer.entity.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {

    Optional<Trainer> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Trainer> findBySpecializationContainingIgnoreCase(
            String specialization);

    List<Trainer> findByAvailabilityIgnoreCase(String availability);

    List<Trainer> findByStatusIgnoreCase(String status);
}
