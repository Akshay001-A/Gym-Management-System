package com.gym.trainer.controller;

import com.gym.trainer.entity.Trainer;
import com.gym.trainer.dto.TrainerWriteRequest;
import com.gym.trainer.service.TrainerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {
    private final TrainerService trainerService;

    public TrainerController(TrainerService trainerService) {
        this.trainerService = trainerService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Trainer> createTrainer(@Valid @RequestBody TrainerWriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trainerService.createTrainer(toTrainer(request)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<Trainer> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(trainerService.getTrainerByEmail(jwt.getSubject()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public ResponseEntity<List<Trainer>> getAllTrainers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(trainerService.getAllTrainers(pageRequest(page, size, "id", "asc")));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public Page<Trainer> getTrainersPage(@RequestParam(required = false) String q,
            @RequestParam(required = false) String specialization, @RequestParam(required = false) String availability,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return trainerService.getTrainersPage(q, specialization, availability, status,
                pageRequest(page, size, sortBy, direction));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public ResponseEntity<Trainer> getTrainerById(@PathVariable Long id) {
        return ResponseEntity.ok(trainerService.getTrainerById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Trainer> updateTrainer(@PathVariable Long id, @Valid @RequestBody TrainerWriteRequest request) {
        return ResponseEntity.ok(trainerService.updateTrainer(id, toTrainer(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTrainer(@PathVariable Long id) {
        trainerService.deleteTrainer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public ResponseEntity<List<Trainer>> searchBySpecialization(@RequestParam String specialization,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(trainerService.searchBySpecialization(specialization, pageRequest(page, size, "name", "asc")));
    }

    @GetMapping("/availability/{availability}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public ResponseEntity<List<Trainer>> getByAvailability(@PathVariable String availability,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(trainerService.getTrainersByAvailability(availability, pageRequest(page, size, "name", "asc")));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public ResponseEntity<List<Trainer>> getByStatus(@PathVariable String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(trainerService.getTrainersByStatus(status, pageRequest(page, size, "name", "asc")));
    }

    private Trainer toTrainer(TrainerWriteRequest request) {
        Trainer trainer = new Trainer();
        trainer.setName(request.name());
        trainer.setEmail(request.email());
        trainer.setMobile(request.mobile());
        trainer.setSpecialization(request.specialization());
        trainer.setExperienceYears(request.experienceYears());
        trainer.setAvailability(request.availability());
        trainer.setStatus(request.status());
        return trainer;
    }

    private Pageable pageRequest(int page, int size, String sortBy, String direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
        }
        if (!List.of("id", "name", "email", "specialization", "experienceYears", "availability", "status").contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        try { return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy)); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort direction"); }
    }
}
