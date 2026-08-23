package com.medgrid.marl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Mathematical fairness and allocation equity metrics for multi-agent systems.
 * Implements Jain's Fairness Index, Gini Coefficient, and Workload Variance.
 */
public class FairnessMetric {

    /**
     * Calculates Jain's Fairness Index for an array of workloads.
     * J(x) = (sum x_i)^2 / (n * sum (x_i^2))
     * Result in [1/n, 1.0].
     */
    public static double computeJainsIndex(double[] workloads) {
        if (workloads == null || workloads.length == 0) return 1.0;
        int n = workloads.length;

        double sum = 0.0;
        double sumSq = 0.0;
        for (double w : workloads) {
            sum += w;
            sumSq += (w * w);
        }

        if (sumSq == 0.0) {
            return 1.0; // All zero is trivially fair
        }

        return (sum * sum) / (n * sumSq);
    }

    public static double computeJainsIndex(Map<String, Integer> agentCaseCounts) {
        if (agentCaseCounts == null || agentCaseCounts.isEmpty()) return 1.0;
        double[] vals = new double[agentCaseCounts.size()];
        int idx = 0;
        for (int count : agentCaseCounts.values()) {
            vals[idx++] = count;
        }
        return computeJainsIndex(vals);
    }

    /**
     * Computes the Gini Coefficient of inequality (0.0 = perfect equality, 1.0 = total inequality).
     */
    public static double computeGiniCoefficient(double[] values) {
        if (values == null || values.length <= 1) return 0.0;
        int n = values.length;
        double[] sorted = Arrays.copyOf(values, n);
        Arrays.sort(sorted);

        double totalSum = 0.0;
        for (double v : sorted) totalSum += v;
        if (totalSum == 0.0) return 0.0;

        double weightedSum = 0.0;
        for (int i = 0; i < n; i++) {
            weightedSum += (i + 1) * sorted[i];
        }

        return (2.0 * weightedSum) / (n * totalSum) - (n + 1.0) / n;
    }

    /**
     * Computes the standard deviation of workload distribution across agents.
     */
    public static double computeWorkloadStdDev(double[] workloads) {
        if (workloads == null || workloads.length <= 1) return 0.0;
        double sum = 0;
        for (double w : workloads) sum += w;
        double mean = sum / workloads.length;

        double varianceSum = 0;
        for (double w : workloads) {
            varianceSum += (w - mean) * (w - mean);
        }
        return Math.sqrt(varianceSum / workloads.length);
    }
}
