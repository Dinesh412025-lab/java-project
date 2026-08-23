package com.medgrid.advisor;

public class MatchResult {
    private final Condition condition;
    private final int matchedSymptomCount;

    public MatchResult(Condition condition, int matchedSymptomCount) {
        this.condition = condition;
        this.matchedSymptomCount = matchedSymptomCount;
    }

    public Condition getCondition() {
        return condition;
    }

    public int getMatchedSymptomCount() {
        return matchedSymptomCount;
    }
}
