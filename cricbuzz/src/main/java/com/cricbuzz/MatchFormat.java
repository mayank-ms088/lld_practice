/*
 * Copyright: ThoughtSpot Inc. 2024
 */

package com.cricbuzz;

import lombok.Getter;

/**
 * INTERFACE_DEFINITION_COMMENTS
 *
 * @author Mayank Sharma (mayank.sharma@thoughtspot.com)
 */
public enum MatchFormat {
    ONE_DAY(50, 1),
    T_20(20, 1),
    TEST(-1, 2);

    @Getter
    private long maxOverLimit;
    @Getter
    private long maxNoOfInningsAllowedPerTeam;

    MatchFormat(final long i, final long j) {
        maxOverLimit = i;
        maxNoOfInningsAllowedPerTeam = j;
    }
}
