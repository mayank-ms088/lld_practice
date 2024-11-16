/*
 * Copyright: ThoughtSpot Inc. 2024
 */

package com.cricbuzz;

import java.util.*;
/**
 * CLASS_DEFINITION_COMMENTS
 *
 * @author Mayank Sharma (mayank.sharma@thoughtspot.com)
 */
public class Over {
    private static int MAX_BALL_PER_OVER = 6;
    private List<Ball> ballsBowled;
    private int validBalls;
    public Over() {
        ballsBowled = new ArrayList<>();
        validBalls = 0;
    }

    public void addBall(final Ball ball) {
        ballsBowled.add(ball);
        if (ball.getBallType().isValidBall()) {
            validBalls++;
        }
    }

    boolean isCompleted() {
        return validBalls == MAX_BALL_PER_OVER;
    }
}
