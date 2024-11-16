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
    private Deque<Bowler> bowlerSequence;
    private Bowler bowler;
    private long totalWickets;
    private long totalExtras;

    public BowlingTeam(
            final List<Person> playing11, final List<Person> onBench, final String teamName) {
        super(playing11, onBench, teamName);
        playing11.forEach(v -> {
            bowlerSequence.add(new Bowler(v));
        });
        bowler = getNextBowler();
    }

    private Bowler getNextBowler() {
        Bowler nextPerson = null;
        while(!bowlerSequence.isEmpty() && !bowlerSequence.peekFirst().areOversLeft()) {
            bowlerSequence.pollFirst();
        }
        if (!bowlerSequence.isEmpty()) {
            nextPerson = bowlerSequence.pollFirst();
        }
        if (nextPerson != null) bowlerSequence.addLast(nextPerson);
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
}
