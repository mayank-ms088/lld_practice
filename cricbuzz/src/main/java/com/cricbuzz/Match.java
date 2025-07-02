/*
 * Copyright: ThoughtSpot Inc. 2024
 */

package com.cricbuzz;

import java.util.ArrayList;
import java.util.List;

/**
 * CLASS_DEFINITION_COMMENTS
 * observer,
 * @author Mayank Sharma (mayank.sharma@thoughtspot.com)
 */
public class Match {
    private long startTime;
    private MatchFormat format;
    private List<Inning> innings;
    private Team teamASquad;
    private Team teamBSquad;

    public Match(final MatchFormat format, final Team teamA, final Team teamB) {
        startTime = System.currentTimeMillis();
        this.format = format;
        this.teamASquad = teamA;
        this.teamBSquad = teamB;
        this.innings = new ArrayList<>();
    }

    public void startMatch() {
        Inning curInning = new Inning(teamASquad, teamBSquad, this.format);
        this.innings.add(curInning);
        while (!curInning.isCompleted()) {
            Ball ball = new Ball();
            curInning.handleNextBall(ball);
        }
    }
}
