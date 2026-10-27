
package com.gym.trainer.controller;

import com.gym.trainer.entity.Trainer;
import com.gym.trainer.service.TrainerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {

    private final TrainerService trainerService;

    public TrainerController(TrainerService trainerService) {
        this.trainerService = trainerService;
    }

    // Only ADMIN can create trainers
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Trainer> createTrainer(
            @Valid @RequestBody Trainer trainer) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainerService.createTrainer(trainer));
    }

    // ADMIN and MEMBER can view the trainer list
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public ResponseEntity<List<Trainer>> getAllTrainers() {
        return ResponseEntity.ok(trainerService.getAllTrainers());
    }

    // ADMIN and MEMBER can view trainer details
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public ResponseEntity<Trainer> getTrainerById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                trainerService.getTrainerById(id));
    }

    // Only ADMIN can update trainer details
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Trainer> updateTrainer(
            @PathVariable Long id,
            @Valid @RequestBody Trainer trainer) {
        return ResponseEntity.ok(
                trainerService.updateTrainer(id, trainer));
    }

    // Only ADMIN can delete trainers
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTrainer(
            @PathVariable Long id) {
        trainerService.deleteTrainer(id);
        return ResponseEntity.noContent().build();
    }

    // ADMIN and MEMBER can search trainers
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public ResponseEntity<List<Trainer>> searchBySpecialization(
            @RequestParam String specialization) {
        return ResponseEntity.ok(
                trainerService.searchBySpecialization(specialization));
    }

    @GetMapping("/availability/{availability}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public ResponseEntity<List<Trainer>> getByAvailability(
            @PathVariable String availability) {
        return ResponseEntity.ok(
                trainerService.getTrainersByAvailability(availability));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public ResponseEntity<List<Trainer>> getByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(
                trainerService.getTrainersByStatus(status));
    }
}
