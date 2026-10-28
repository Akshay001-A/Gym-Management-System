package com.gym.trainer.service;

import com.gym.trainer.entity.Trainer;
import com.gym.trainer.exception.DuplicateTrainerEmailException;
import com.gym.trainer.exception.TrainerNotFoundException;
import com.gym.trainer.repository.TrainerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TrainerService {
    private final TrainerRepository trainerRepository;

    public TrainerService(TrainerRepository trainerRepository) {
        this.trainerRepository = trainerRepository;
    }

    public Trainer createTrainer(Trainer trainer) {
        if (trainerRepository.existsByEmail(trainer.getEmail())) {
            throw new DuplicateTrainerEmailException(trainer.getEmail());
        }
        return trainerRepository.save(trainer);
    }

    public Trainer getTrainerByEmail(String email) {
        return trainerRepository.findByEmail(email)
                .orElseThrow(() -> new TrainerNotFoundException("Trainer profile not found for the authenticated account"));
    }

    public List<Trainer> getAllTrainers(Pageable pageable) {
        return trainerRepository.findAll(pageable).getContent();
    }

    public Page<Trainer> getTrainersPage(String query, String specialization, String availability, String status, Pageable pageable) {
        Specification<Trainer> specification = (root, criteriaQuery, builder) -> builder.conjunction();
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase() + "%";
            specification = specification.and((root, criteriaQuery, builder) -> builder.or(
                    builder.like(builder.lower(root.get("name")), like),
                    builder.like(builder.lower(root.get("email")), like),
                    builder.like(builder.lower(root.get("specialization")), like)));
        }
        if (specialization != null && !specialization.isBlank()) {
            String like = "%" + specialization.trim().toLowerCase() + "%";
            specification = specification.and((root, criteriaQuery, builder) ->
                    builder.like(builder.lower(root.get("specialization")), like));
        }
        if (availability != null && !availability.isBlank()) {
            String expected = availability.trim().toLowerCase();
            specification = specification.and((root, criteriaQuery, builder) ->
                    builder.equal(builder.lower(root.get("availability")), expected));
        }
        if (status != null && !status.isBlank()) {
            String expected = status.trim().toLowerCase();
            specification = specification.and((root, criteriaQuery, builder) ->
                    builder.equal(builder.lower(root.get("status")), expected));
        }
        return trainerRepository.findAll(specification, pageable);
    }

    public Trainer getTrainerById(Long id) {
        return trainerRepository.findById(id).orElseThrow(() -> new TrainerNotFoundException(id));
    }

    public Trainer updateTrainer(Long id, Trainer updatedTrainer) {
        Trainer existing = getTrainerById(id);
        trainerRepository.findByEmail(updatedTrainer.getEmail())
                .filter(trainer -> !trainer.getId().equals(id))
                .ifPresent(trainer -> { throw new DuplicateTrainerEmailException(updatedTrainer.getEmail()); });
        existing.setName(updatedTrainer.getName());
        existing.setEmail(updatedTrainer.getEmail());
        existing.setMobile(updatedTrainer.getMobile());
        existing.setSpecialization(updatedTrainer.getSpecialization());
        existing.setExperienceYears(updatedTrainer.getExperienceYears());
        existing.setAvailability(updatedTrainer.getAvailability());
        existing.setStatus(updatedTrainer.getStatus());
        return trainerRepository.save(existing);
    }

    public void deleteTrainer(Long id) {
        trainerRepository.delete(getTrainerById(id));
    }

    public List<Trainer> searchBySpecialization(String specialization, Pageable pageable) {
        return getTrainersPage(null, specialization, null, null, pageable).getContent();
    }

    public List<Trainer> getTrainersByAvailability(String availability, Pageable pageable) {
        return getTrainersPage(null, null, availability, null, pageable).getContent();
    }

    public List<Trainer> getTrainersByStatus(String status, Pageable pageable) {
        return getTrainersPage(null, null, null, status, pageable).getContent();
    }
}
