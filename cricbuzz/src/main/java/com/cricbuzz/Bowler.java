/*
 * Copyright: ThoughtSpot Inc. 2024
 */

package com.cricbuzz;

import java.util.ArrayList;
import java.util.List;

/**
 * CLASS_DEFINITION_COMMENTS
 *
 * @author Mayank Sharma (mayank.sharma@thoughtspot.com)
 */
public class Bowler extends Player {
    private long maxOverAllowedPerBowler = 10;
    private long wickets;
    private double economyRate;
    private List<Over> overBowled;
    private Over currentOver;
    public Bowler(final Person nextBowler) {
        super(nextBowler);
        wickets = 0;
        economyRate = 0;
        overBowled = new ArrayList<>();
    }

    @Override
    public void updateScore(final Ball ball) {
        currentOver.addBall(ball);
        if (currentOver.isCompleted()) {
            overBowled.add(currentOver);
            currentOver = new Over();
        }
        if (ball.getBallType() == BallType.WICKET) {
            wickets++;
        }
    }

    boolean isCurrentOverCompleted() {
        return currentOver.isCompleted();
    }

    boolean areOversLeft() {
        return !(overBowled.size() == maxOverAllowedPerBowler
                && currentOver.isCompleted());
    }
}
