
package com.gym.trainer.exception;

public class TrainerNotFoundException extends RuntimeException {

    public TrainerNotFoundException(Long id) {
        super("Trainer not found with ID: " + id);
    }
}
