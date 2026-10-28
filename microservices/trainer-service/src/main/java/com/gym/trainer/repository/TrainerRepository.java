package com.gym.trainer.repository;

import com.gym.trainer.entity.Trainer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TrainerRepository extends JpaRepository<Trainer, Long>, JpaSpecificationExecutor<Trainer> {
    Optional<Trainer> findByEmail(String email);
    boolean existsByEmail(String email);
}
