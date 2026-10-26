
package com.gym.trainer.controller;

import com.gym.trainer.entity.Trainer;
import com.gym.trainer.service.TrainerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {

    private final TrainerService trainerService;

    public TrainerController(TrainerService trainerService) {
        this.trainerService = trainerService;
    }

    @PostMapping
    public ResponseEntity<Trainer> createTrainer(
            @Valid @RequestBody Trainer trainer) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainerService.createTrainer(trainer));
    }

    @GetMapping
    public ResponseEntity<List<Trainer>> getAllTrainers() {
        return ResponseEntity.ok(trainerService.getAllTrainers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trainer> getTrainerById(
            @PathVariable Long id) {
        return ResponseEntity.ok(trainerService.getTrainerById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Trainer> updateTrainer(
            @PathVariable Long id,
            @Valid @RequestBody Trainer trainer) {
        return ResponseEntity.ok(
                trainerService.updateTrainer(id, trainer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrainer(
            @PathVariable Long id) {
        trainerService.deleteTrainer(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/search")
    public ResponseEntity<List<Trainer>> searchBySpecialization(
            @RequestParam String specialization) {
        return ResponseEntity.ok(
                trainerService.searchBySpecialization(specialization));
    }

    @GetMapping("/availability/{availability}")
    public ResponseEntity<List<Trainer>> getByAvailability(
            @PathVariable String availability) {
        return ResponseEntity.ok(
                trainerService.getTrainersByAvailability(availability));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Trainer>> getByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(
                trainerService.getTrainersByStatus(status));
    }

}
