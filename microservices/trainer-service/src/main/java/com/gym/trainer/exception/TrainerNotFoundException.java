
package com.gym.trainer.exception;

public class TrainerNotFoundException extends RuntimeException {

    public TrainerNotFoundException(String message) {
        super(message);
    }

    public TrainerNotFoundException(Long id) {
        super("Trainer not found with ID: " + id);
    }
}
