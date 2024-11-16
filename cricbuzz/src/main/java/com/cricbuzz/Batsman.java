/*
 * Copyright: ThoughtSpot Inc. 2024
 */

package com.cricbuzz;

/**
 * CLASS_DEFINITION_COMMENTS
 *
 * @author Mayank Sharma (mayank.sharma@thoughtspot.com)
 */
public class Batsman extends Player {
    private long runs;
    private long sixCount;
    private long fourCount;
    private boolean isOut;
    public Batsman(final Person nextPerson) {
        super(nextPerson);
        runs = 0;
        sixCount = 0;
        fourCount = 0;
        isOut = false;
    }

    @Override
    public void updateScore(final Ball ball) {
        if (ball.getBallType() != BallType.WICKET && !ball.areBys() && !ball.areLegBys()) {
            runs += ball.getRunScored();
            sixCount += (ball.getRunType() == RunType.SIX) ? 1 : 0;
            sixCount += (ball.getRunType() == RunType.FOUR) ? 1 : 0;
        } else {
            isOut = true;
        }
    }

    public boolean isOut() {
        return isOut;
    }
}
