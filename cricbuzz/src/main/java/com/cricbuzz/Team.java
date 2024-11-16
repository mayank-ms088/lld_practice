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
public class Team {
    private List<Person> playing11;
    private List<Person> onBench;
    private String teamName;
    private boolean isWinner;

    public Team(final List<Person> playing11, final List<Person> onBench, final String teamName) {
        this.onBench = onBench;
        this.playing11 = playing11;
        isWinner = false;
        this.teamName = teamName;
    }

    public boolean isWinner() {
        return isWinner;
    }

    public void setIsWinner(final boolean isWinner) {
        this.isWinner = isWinner;
    }

    public List<Person> getPlaying11() {
        return playing11;
    }

    public List<Person> getOnBench() {
        return onBench;
    }

    public String getName() {
        return teamName;
    }
}
