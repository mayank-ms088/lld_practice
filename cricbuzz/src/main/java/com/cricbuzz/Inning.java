/*
 * Copyright: ThoughtSpot Inc. 2024
 */

package com.cricbuzz;

import lombok.Getter;

/**
 * CLASS_DEFINITION_COMMENTS
 *
 * @author Mayank Sharma (mayank.sharma@thoughtspot.com)
 */
public class Inning {
    private BattingTeam battingTeam;
    private BowlingTeam bowlingTeam;
    @Getter
    private boolean completed;
    public Inning(final Team battingTeam, final Team bowlingTeam) {
        this.battingTeam = new BattingTeam(battingTeam.getPlaying11(), battingTeam.getOnBench(),
                battingTeam.getName());
        this.bowlingTeam = new BowlingTeam(bowlingTeam.getPlaying11(), bowlingTeam.getOnBench(),
                bowlingTeam.getName());
        completed = false;
    }

    public void handleNextBall(final Ball ball) {
        battingTeam.updateScore(ball);
        bowlingTeam.updateScore(ball);
        if (battingTeam.isAllOut() || !bowlingTeam.areOversLeft()) {
            completed = true;
        }
    }

}
