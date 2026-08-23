package com.medgrid.patterns;

import com.medgrid.model.CaseSeverity;

import java.util.List;

/**
 * Spatiotemporal Hawkes Process Intensity Estimator.
 * Models self-exciting, cascading incident patterns where an incident at (s_i, t_i)
 * increases the conditional intensity of future incidents in spatio-temporal vicinity.
 *
 * Intensity: lambda(s, t) = mu_0 + sum_{t_i < t} alpha * exp(-beta * (t - t_i)) * exp(- ||s - s_i||^2 / (2 * sigma^2))
 */
public class HawkesIntensityEstimator {
    private final double baseIntensity;     // mu_0: baseline spontaneous rate
    private final double alpha;             // Excitation scaling / branching ratio
    private final double beta;              // Temporal decay rate (1/ms)
    private final double sigma;             // Spatial kernel bandwidth (distance units)

    public HawkesIntensityEstimator(double baseIntensity, double alpha, double beta, double sigma) {
        this.baseIntensity = baseIntensity;
        this.alpha = alpha;
        this.beta = beta;
        this.sigma = sigma;
    }

    public HawkesIntensityEstimator() {
        // Defaults: decay over ~30 seconds (30000ms), spatial bandwidth = 3.5 km
        this(0.05, 0.75, 1.0 / 30000.0, 3.5);
    }

    /**
     * Computes the conditional intensity lambda(x, y, t) given historical incidents.
     */
    public double computeIntensity(double x, double y, long currentTimestampMs, List<IncidentRecord> history) {
        double intensity = baseIntensity;
        if (history == null || history.isEmpty()) {
            return intensity;
        }

        double twoSigmaSq = 2.0 * sigma * sigma;

        for (IncidentRecord inc : history) {
            long dt = currentTimestampMs - inc.getTimestampMs();
            if (dt < 0) continue; // Future event relative to query time

            double dx = x - inc.getX();
            double dy = y - inc.getY();
            double distSq = dx * dx + dy * dy;

            // Severity weighting: critical events trigger stronger excitation
            double sevWeight = (inc.getSeverity() == CaseSeverity.CRITICAL) ? 2.0 :
                               (inc.getSeverity() == CaseSeverity.HIGH) ? 1.5 : 1.0;

            double temporalKernel = Math.exp(-beta * dt);
            double spatialKernel = Math.exp(-distSq / twoSigmaSq);

            intensity += alpha * sevWeight * temporalKernel * spatialKernel;
        }

        return intensity;
    }

    /**
     * Normalized risk index mapped to [0.0, 1.0].
     */
    public double computeNormalizedRisk(double x, double y, long currentTimestampMs, List<IncidentRecord> history) {
        double raw = computeIntensity(x, y, currentTimestampMs, history);
        // Sigmoidal / saturation scaling: 1 - exp(-raw)
        return 1.0 - Math.exp(-raw);
    }

    public double getBaseIntensity() { return baseIntensity; }
    public double getAlpha() { return alpha; }
    public double getBeta() { return beta; }
    public double getSigma() { return sigma; }
}
