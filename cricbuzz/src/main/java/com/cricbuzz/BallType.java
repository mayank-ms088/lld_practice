/*
 * Copyright: ThoughtSpot Inc. 2024
 */

package com.cricbuzz;

/**
 * INTERFACE_DEFINITION_COMMENTS
 *
 * @author Mayank Sharma (mayank.sharma@thoughtspot.com)
 */
public enum BallType {
    NO_BALL(false),
    WIDE_BALL(false),
    DEAD_BALL(false),
    WICKET(true),
    NORMAL(true);
    boolean validBall;

    BallType(final boolean b) {
        this.validBall = b;
    }

    boolean isValidBall() {
        return this.validBall;
    }
}
