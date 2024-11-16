/*
 * Copyright: ThoughtSpot Inc. 2024
 */

package com.cricbuzz;

/**
 * CLASS_DEFINITION_COMMENTS
 *
 * @author Mayank Sharma (mayank.sharma@thoughtspot.com)
 */
public class Ball {
    private BallType type;
    private long runScored;
    private RunType runType;
    private Wicket wicket;
    private boolean areLegBys;
    private boolean areBys;

    public BallType getBallType() {
        return type;
    }

    public long getRunScored() {
        return runScored;
    }

    public RunType getRunType() {
        return runType;
    }

    public Wicket getWicket() {
        return wicket;
    }

    public boolean areLegBys() {
        return areLegBys;
    }

    public boolean areBys() {
        return areBys;
    }
}
