/*
 * Copyright: ThoughtSpot Inc. 2024
 */

package com.cricbuzz;

/**
 * CLASS_DEFINITION_COMMENTS
 *
 * @author Mayank Sharma (mayank.sharma@thoughtspot.com)
 */
public abstract class Player {
    private Person person;
    public Player(final Person nextPerson) {
        this.person = nextPerson;
    }
    public abstract void updateScore(final Ball ball);
}
