
package com.gym.trainer.exception;

public class DuplicateTrainerEmailException extends RuntimeException {

    public DuplicateTrainerEmailException(String email) {
        super("A trainer with email '" + email + "' already exists");
    }
}
