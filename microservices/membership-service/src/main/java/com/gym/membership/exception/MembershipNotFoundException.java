
package com.gym.membership.exception;

public class MembershipNotFoundException extends RuntimeException {

    public MembershipNotFoundException(Long id) {
        super("Membership not found with ID: " + id);
    }
}
