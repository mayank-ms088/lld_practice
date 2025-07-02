/*
 * Copyright: ThoughtSpot Inc. 2024
 */

package com.cricbuzz;

import java.util.Deque;
import java.util.List;

/**
 * CLASS_DEFINITION_COMMENTS
 *
 * @author Mayank Sharma (mayank.sharma@thoughtspot.com)
 */
public class BowlingTeam extends Team {
    private List<Bowler> bowlerList;
    private Bowler bowler;
    private long totalWickets;
    private long totalExtras;
    private long oversLeft;
    private MatchFormat format;
    public BowlingTeam(
            final List<Person> playing11, final List<Person> onBench, final String teamName,
            final MatchFormat format) {
        super(playing11, onBench, teamName);
        oversLeft = format.getMaxOverLimit();
        playing11.forEach(v -> {
            bowlerList.add(new Bowler(v));
        });
        bowler = getNextBowler();
    }

    private Bowler getNextBowler() {
        oversLeft--;
        if (oversLeft < 0) return null;
        Bowler nextPerson = bow
        return nextPerson;
    }

    public void updateScore(final Ball ball) {
        if (bowler == null) return;
        if (ball.getBallType() == BallType.WICKET) {
            totalWickets++;
        } else if (ball.areBys() || ball.areLegBys()) {
            totalExtras += ball.getRunScored();
        }
        bowler.updateScore(ball);
        if (bowler.isCurrentOverCompleted()) {
            bowler = getNextBowler();
        }
    }

    public boolean areOversLeft() {
        return bowler != null;
    }

    public long getTotalWickets() {
        return totalWickets;
    }

    public long getTotalExtras() {
        return totalExtras;
    }
}
