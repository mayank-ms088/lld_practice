/*
 * Copyright: ThoughtSpot Inc. 2024
 */

package com.cricbuzz;

import java.util.List;
import java.util.Queue;

import lombok.Getter;

/**
 * CLASS_DEFINITION_COMMENTS
 *
 * @author Mayank Sharma (mayank.sharma@thoughtspot.com)
 */
public class BattingTeam extends Team {
    private Queue<Batsman> battingOrder;
    private Batsman striker;
    private Batsman nonStriker;
    @Getter
    private boolean isAllOut;
    @Getter
    private long totalRuns;

    public BattingTeam(final List<Person> playing11, final List<Person> onBench,
            final Queue<Batsman> battingOrder, final String teamName) {
        super(playing11, onBench, teamName);
        this.battingOrder = battingOrder;
        isAllOut = false;
        striker = getNextBatter();
        nonStriker = getNextBatter();
    }

    private Batsman getNextBatter() {
        return battingOrder.poll();
    }

    private void changeStrike() {
        Batsman temp = striker;
        striker = nonStriker;
        nonStriker = temp;
    }

    public void updateScore(final Ball ball) {
        striker.updateScore(ball);
        if (ball.getBallType() == BallType.WICKET) {
            if (striker.isOut()) {
                if (!battingOrder.isEmpty()) {
                    striker = getNextBatter();
                } else {
                    isAllOut = true;
                }
            }
        } else if (ball.getRunType() == RunType.SINGLE) {
            changeStrike();
        }
    }


}
