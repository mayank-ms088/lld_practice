/*
 * Copyright: ThoughtSpot Inc. 2024
 */

package com.cricbuzz;

import java.util.ArrayDeque;
import java.util.Queue;

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
    private MatchFormat format;
    public Inning(final Team battingTeam, final Team bowlingTeam, final MatchFormat format) {
        Queue<Batsman> queue = new ArrayDeque<>();
        for (Person person : battingTeam.getPlaying11()) {
            queue.add(new Batsman(person));
        }
        this.battingTeam = new BattingTeam(battingTeam.getPlaying11(), battingTeam.getOnBench(),
                queue, battingTeam.getName());
        this.bowlingTeam = new BowlingTeam(bowlingTeam.getPlaying11(), bowlingTeam.getOnBench(),
                bowlingTeam.getName(), format);
        completed = false;
    }

    public void handleNextBall(final Ball ball) {
        battingTeam.updateScore(ball);
        bowlingTeam.updateScore(ball);
        if (battingTeam.isAllOut() || !bowlingTeam.areOversLeft()) {
            completed = true;
        }
    }

    public void getScore() {
        System.out.println("Batting Team: " + battingTeam.getName());
        System.out.println("Total Runs: " + battingTeam.getTotalRuns());
        System.out.println("Total Wickets: " + bowlingTeam.getTotalWickets());
        System.out.println("Total Extras: " + bowlingTeam.getTotalExtras());
    }

}
