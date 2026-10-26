
package com.gym.trainer.service;

import com.gym.trainer.entity.Trainer;
import com.gym.trainer.exception.DuplicateTrainerEmailException;
import com.gym.trainer.exception.TrainerNotFoundException;
import com.gym.trainer.repository.TrainerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainerService {

    private final TrainerRepository trainerRepository;

    public TrainerService(TrainerRepository trainerRepository) {
        this.trainerRepository = trainerRepository;
    }

    // Create a new trainer
    public Trainer createTrainer(Trainer trainer) {

        if (trainerRepository.existsByEmail(trainer.getEmail())) {
            throw new DuplicateTrainerEmailException(
                    trainer.getEmail()
            );
        }

        return trainerRepository.save(trainer);
    }

    // Get all trainers
    public List<Trainer> getAllTrainers() {
        return trainerRepository.findAll();
    }

    // Get trainer by ID
    public Trainer getTrainerById(Long id) {
        return trainerRepository.findById(id)
                .orElseThrow(() -> new TrainerNotFoundException(id));
    }

    // Update an existing trainer
    public Trainer updateTrainer(Long id, Trainer updatedTrainer) {

        Trainer existing = getTrainerById(id);

        trainerRepository.findByEmail(updatedTrainer.getEmail())
                .filter(trainer -> !trainer.getId().equals(id))
                .ifPresent(trainer -> {
                    throw new DuplicateTrainerEmailException(
                            updatedTrainer.getEmail()
                    );
                });

        existing.setName(updatedTrainer.getName());
        existing.setEmail(updatedTrainer.getEmail());
        existing.setMobile(updatedTrainer.getMobile());
        existing.setSpecialization(updatedTrainer.getSpecialization());
        existing.setExperienceYears(updatedTrainer.getExperienceYears());
        existing.setAvailability(updatedTrainer.getAvailability());
        existing.setStatus(updatedTrainer.getStatus());

        return trainerRepository.save(existing);
    }

    // Delete a trainer
    public void deleteTrainer(Long id) {
        Trainer trainer = getTrainerById(id);
        trainerRepository.delete(trainer);
    }



    // Search trainers by specialization
    public List<Trainer> searchBySpecialization(String specialization) {
        return trainerRepository
                .findBySpecializationContainingIgnoreCase(specialization);
    }

    // Find trainers by availability
    public List<Trainer> getTrainersByAvailability(String availability) {
        return trainerRepository.findByAvailabilityIgnoreCase(availability);
    }

    // Find trainers by status
    public List<Trainer> getTrainersByStatus(String status) {
        return trainerRepository.findByStatusIgnoreCase(status);
    }

}
