let currentCurrency = 'USD';
const exchangeRate = 83.0; // 1 USD = 83 INR
let authToken = null;

// Login Logic
document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const user = document.getElementById('login-username').value;
    const pass = document.getElementById('login-password').value;
    const btn = document.getElementById('login-btn');
    const err = document.getElementById('login-error');
    
    btn.textContent = 'Authenticating...';
    btn.disabled = true;
    err.style.display = 'none';
    
    try {
        const response = await fetch('/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user, password: pass })
        });
        
        const data = await response.json();
        
        if (response.ok && data.status === 'success') {
            authToken = data.token;
            document.getElementById('login-overlay').style.display = 'none';
            document.getElementById('app-container').style.display = 'flex';
            
            // Start periodic polling
            setInterval(fetchDashboardData, 1000);
            setInterval(fetchAI2Data, 2000);
            fetchDashboardData();
            fetchHistoryData();
            fetchAI2Data();
            runTriagePrediction("chest pain, shortness of breath, sweating");
        } else {
            err.textContent = data.error || 'Login failed';
            err.style.display = 'block';
        }
    } catch (error) {
        err.textContent = 'Network error connecting to server';
        err.style.display = 'block';
    } finally {
        btn.textContent = 'Launch System Dashboard';
        btn.disabled = false;
    }
});

// Format money based on active currency
function formatMoney(usdAmount) {
    if (currentCurrency === 'INR') {
        const inrAmount = usdAmount * exchangeRate;
        return '₹' + inrAmount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
    return '$' + usdAmount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// Navigation Logic
document.querySelectorAll('.nav-links li').forEach(link => {
    link.addEventListener('click', () => {
        document.querySelectorAll('.nav-links li').forEach(l => l.classList.remove('active'));
        link.classList.add('active');
        
        const targetView = link.getAttribute('data-view');
        document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
        const activeSection = document.getElementById('view-' + targetView);
        if (activeSection) activeSection.classList.add('active');
        
        const headingEl = document.getElementById('page-heading');
        if (headingEl) {
            const rawTitle = link.textContent.trim();
            headingEl.textContent = rawTitle.replace(/[\u{1F300}-\u{1F9FF}]/gu, '').trim();
        }
    });
});

// Currency Toggle Logic
const currencySwitch = document.getElementById('currency-switch');
currencySwitch.addEventListener('change', (e) => {
    if (e.target.checked) {
        currentCurrency = 'INR';
        document.getElementById('label-usd').classList.remove('active');
        document.getElementById('label-inr').classList.add('active');
    } else {
        currentCurrency = 'USD';
        document.getElementById('label-inr').classList.remove('active');
        document.getElementById('label-usd').classList.add('active');
    }
    fetchDashboardData();
});

// Modal Logic
const modal = document.getElementById('hospital-modal');
const closeBtn = document.getElementById('modal-close');

if (closeBtn) {
    closeBtn.onclick = function() {
        modal.style.display = "none";
    };
}

window.onclick = function(event) {
    if (event.target == modal) {
        modal.style.display = "none";
    }
};

let latestHospitals = [];

async function fetchDashboardData() {
    try {
        const response = await fetch('/api/dashboard');
        if (!response.ok) return;
        const data = await response.json();
        renderDashboard(data);
        renderHospitalsView(data);
        renderAmbulancesView(data);
        renderAnalyticsView(data);
    } catch (error) {
        console.error('Error fetching dashboard data:', error);
    }
}

function renderDashboard(data) {
    if (!data || !data.metrics) return;
    document.getElementById('total-cases').textContent = data.metrics.totalCases;
    document.getElementById('avg-response').textContent = data.metrics.avgResponseTime.toFixed(2) + ' ms';
    
    const netFinance = data.metrics.totalRevenue - data.metrics.totalCost;
    const netEl = document.getElementById('net-finance');
    netEl.textContent = formatMoney(netFinance);
    netEl.style.color = netFinance >= 0 ? 'var(--accent-green)' : 'var(--accent-red)';

    const logsList = document.getElementById('logs-list');
    if (logsList && data.logs) {
        logsList.innerHTML = '';
        data.logs.forEach(log => {
            logsList.innerHTML += `<li>${log}</li>`;
        });
    }
}

function renderHospitalsView(data) {
    const hospContainer = document.getElementById('hospitals-container');
    if (!hospContainer || !data.hospitals) return;
    hospContainer.innerHTML = '';
    latestHospitals = data.hospitals;

    data.hospitals.forEach(h => {
        let loadClass = 'green';
        if (h.load > 50) loadClass = 'orange';
        if (h.load > 80) loadClass = 'red';

        const card = `
            <div class="card clickable-card" onclick="openHospitalModal('${h.id}')">
                <div class="card-header">
                    <h3>${h.id}</h3>
                    <span class="badge ${loadClass}">${h.load.toFixed(1)}% Load</span>
                </div>
                <div class="stat-row"><span>ER Available:</span> <span>${h.er_available}</span></div>
                <div class="stat-row"><span>ICU Available:</span> <span>${h.icu_available}</span></div>
                <div class="stat-row"><span>Revenue:</span> <span style="color:var(--accent-green)">${formatMoney(h.revenue)}</span></div>
            </div>
        `;
        hospContainer.innerHTML += card;
    });
}

function openHospitalModal(hospId) {
    const hosp = latestHospitals.find(h => h.id === hospId);
    if (!hosp) return;

    document.getElementById('modal-hosp-name').textContent = hosp.id + " Details";

    const docList = document.getElementById('modal-doctors-list');
    docList.innerHTML = '';
    for (const [spec, count] of Object.entries(hosp.doctors)) {
        docList.innerHTML += `<li><span>${spec}</span> <span style="color:var(--accent-blue)">${count} Available</span></li>`;
    }

    const medList = document.getElementById('modal-meds-list');
    medList.innerHTML = '';
    for (const [med, count] of Object.entries(hosp.medicines)) {
        medList.innerHTML += `<li><span>${med}</span> <span style="color:var(--accent-green)">${count} Units</span></li>`;
    }

    modal.style.display = "block";
}

function renderAmbulancesView(data) {
    const ambContainer = document.getElementById('ambulances-container');
    if (!ambContainer || !data.ambulances) return;
    ambContainer.innerHTML = '';
    data.ambulances.forEach(a => {
        let stateClass = 'blue';
        if (a.state === 'IDLE') stateClass = 'green';
        else if (a.state.includes('ROUTE')) stateClass = 'orange';

        const card = `
            <div class="card">
                <div class="card-header">
                    <h3>${a.id}</h3>
                    <span class="badge ${stateClass}">${a.state.replace(/_/g, ' ')}</span>
                </div>
                <div class="stat-row"><span>Location:</span> <span>${a.location}</span></div>
                <div class="stat-row"><span>Operating Cost:</span> <span style="color:var(--accent-red)">-${formatMoney(a.cost)}</span></div>
            </div>
        `;
        ambContainer.innerHTML += card;
    });
}

function renderAnalyticsView(data) {
    if (!data.history || data.history.length < 2) return;

    const svg = document.getElementById('revenue-graph');
    if (!svg) return;
    const width = svg.clientWidth || 600;
    const height = 400;
    const padding = 40;

    const history = data.history;
    const yValues = history.map(d => {
        let val = d.netRevenue;
        if (currentCurrency === 'INR') val *= exchangeRate;
        return val;
    });
    
    // Linear Regression
    let sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
    const n = yValues.length;
    for (let i = 0; i < n; i++) {
        sumX += i;
        sumY += yValues[i];
        sumXY += i * yValues[i];
        sumX2 += i * i;
    }
    const denom = n * sumX2 - sumX * sumX;
    const slope = denom === 0 ? 0 : (n * sumXY - sumX * sumY) / denom;
    const intercept = (sumY - slope * sumX) / n;

    const futureSteps = 10;
    const totalSteps = n + futureSteps;
    
    const minY = Math.min(0, ...yValues, intercept + slope * totalSteps);
    const maxY = Math.max(...yValues, intercept + slope * totalSteps);
    const yRange = maxY - minY === 0 ? 1 : maxY - minY;

    function getX(i) {
        return padding + (i / (totalSteps - 1)) * (width - 2 * padding);
    }
    
    function getY(val) {
        return height - padding - ((val - minY) / yRange) * (height - 2 * padding);
    }

    let svgContent = '';
    for(let i=0; i<=5; i++) {
        const yLine = height - padding - (i/5) * (height - 2 * padding);
        const yVal = minY + (i/5) * yRange;
        svgContent += `<line x1="${padding}" y1="${yLine}" x2="${width-padding}" y2="${yLine}" stroke="rgba(255,255,255,0.1)" />`;
        svgContent += `<text x="${padding - 10}" y="${yLine + 4}" fill="#94a3b8" font-size="12" text-anchor="end">${Math.round(yVal)}</text>`;
    }

    let histPoints = '';
    for(let i = 0; i < n; i++) {
        histPoints += `${getX(i)},${getY(yValues[i])} `;
    }
    svgContent += `<polyline points="${histPoints}" fill="none" stroke="var(--accent-blue)" stroke-width="3" />`;
    
    for(let i = 0; i < n; i++) {
        svgContent += `<circle cx="${getX(i)}" cy="${getY(yValues[i])}" r="4" fill="var(--accent-blue)" />`;
    }

    const lastX = getX(n - 1);
    const lastY = getY(yValues[n - 1]);
    const predEndX = getX(totalSteps - 1);
    const predEndY = getY(intercept + slope * (totalSteps - 1));
    
    svgContent += `<line x1="${lastX}" y1="${lastY}" x2="${predEndX}" y2="${predEndY}" stroke="var(--accent-green)" stroke-width="3" stroke-dasharray="8,6" />`;
    svgContent += `<circle cx="${predEndX}" cy="${predEndY}" r="5" fill="var(--accent-green)" />`;
    svgContent += `<text x="${predEndX}" y="${predEndY - 15}" fill="var(--accent-green)" font-size="12" text-anchor="middle" font-weight="bold">Predicted</text>`;

    svg.innerHTML = svgContent;
}

async function fetchHistoryData() {
    try {
        const response = await fetch('/api/history');
        if (!response.ok) return;
        const data = await response.json();
        
        const totalCasesEl = document.getElementById('hist-total-cases');
        const avgRespEl = document.getElementById('hist-avg-response');
        const succRateEl = document.getElementById('hist-success-rate');
        
        if (totalCasesEl) totalCasesEl.textContent = data.totalCases;
        if (avgRespEl) avgRespEl.textContent = data.avgResponseTimeMs.toFixed(2) + ' ms';
        if (succRateEl) succRateEl.textContent = data.successRate.toFixed(1) + '%';
    } catch (error) {
        console.error('Error fetching history data:', error);
    }
}

// -------------------------------------------------------------
// MedGrid-AI 2.0 Telemetry & Benchmarking
// -------------------------------------------------------------
async function fetchAI2Data() {
    try {
        const res = await fetch('/api/ai2');
        if (!res.ok) return;
        const data = await res.json();

        // 1. Hotspots & MARL metrics
        if (data.marl) {
            document.getElementById('ai2-jains').textContent = data.marl.jainsFairness.toFixed(4);
            document.getElementById('ai2-gini').textContent = data.marl.giniCoefficient.toFixed(4);
        }
        if (data.hotspots) {
            document.getElementById('ai2-hotspots').textContent = data.hotspots.activeClusters;
            renderHotspotZones(data.hotspots.zones);
        }
        if (data.marl && data.marl.recentNegotiations) {
            renderNegotiations(data.marl.recentNegotiations);
        }
        if (data.benchmark) {
            renderBenchmarkTable(data.benchmark);
        }
    } catch (err) {
        console.error('Error fetching AI2 data:', err);
    }
}

function renderHotspotZones(zones) {
    const container = document.getElementById('zones-grid');
    if (!container || !zones) return;
    container.innerHTML = '';

    zones.forEach(z => {
        const riskPct = Math.min(100, Math.round(z.riskScore * 100));
        let color = '#38bdf8';
        if (riskPct > 40) color = '#fbbf24';
        if (riskPct > 70) color = '#ef4444';

        const card = `
            <div class="zone-card ${z.isHotspot ? 'hotspot' : ''}">
                <div class="zone-title">${z.node}</div>
                <div class="zone-risk-text">Risk: ${z.riskScore.toFixed(3)}</div>
                <div class="zone-bar-bg">
                    <div class="zone-bar-fill" style="width: ${riskPct}%; background: ${color}"></div>
                </div>
            </div>
        `;
        container.innerHTML += card;
    });
}

function renderNegotiations(negotiations) {
    const list = document.getElementById('negotiation-list');
    if (!list) return;
    list.innerHTML = '';

    if (negotiations.length === 0) {
        list.innerHTML = '<div style="color:#94a3b8; font-size:0.85rem; padding: 0.5rem 0;">Awaiting multi-agent dispatch auctions...</div>';
        return;
    }

    negotiations.forEach(n => {
        const item = `
            <div class="negotiation-item">
                <div>
                    <strong>Case ${n.caseId}</strong> (${n.severity})
                </div>
                <div>
                    Winner: <span class="negotiation-winner">${n.winner}</span> (Bid: ${n.winningBid.toFixed(1)})
                </div>
                <div style="color: #94a3b8; font-size: 0.8rem;">
                    Jain's: ${n.jainsIndex.toFixed(3)}
                </div>
            </div>
        `;
        list.innerHTML += item;
    });
}

function renderBenchmarkTable(benchmarks) {
    const tbody = document.getElementById('benchmark-table-body');
    if (!tbody || !benchmarks) return;
    tbody.innerHTML = '';

    benchmarks.forEach(b => {
        const row = `
            <tr>
                <td><strong>${b.scenario}</strong></td>
                <td>${b.baselineMean.toFixed(1)} ms</td>
                <td style="color:#38bdf8; font-weight:600;">${b.ai2Mean.toFixed(1)} ms</td>
                <td>${b.baselineFairness.toFixed(4)}</td>
                <td style="color:#10b981; font-weight:600;">${b.ai2Fairness.toFixed(4)}</td>
                <td>${b.baselineCritical.toFixed(1)} ms / <span style="color:#38bdf8">${b.ai2Critical.toFixed(1)} ms</span></td>
            </tr>
        `;
        tbody.innerHTML += row;
    });
}

// -------------------------------------------------------------
// Explainable Triage Interaction
// -------------------------------------------------------------
document.querySelectorAll('.symptom-tag').forEach(tagBtn => {
    tagBtn.addEventListener('click', () => {
        const input = document.getElementById('triage-input');
        const tag = tagBtn.getAttribute('data-tag');
        if (input.value.trim().length === 0) {
            input.value = tag;
        } else if (!input.value.toLowerCase().includes(tag.toLowerCase())) {
            input.value = input.value.trim() + ', ' + tag;
        }
        runTriagePrediction(input.value);
    });
});

document.getElementById('triage-form').addEventListener('submit', (e) => {
    e.preventDefault();
    const input = document.getElementById('triage-input').value;
    runTriagePrediction(input);
});

async function runTriagePrediction(symptoms) {
    if (!symptoms || symptoms.trim().length === 0) return;
    const btn = document.getElementById('triage-btn');
    if (btn) btn.textContent = 'Analyzing Features...';

    try {
        const res = await fetch('/api/triage?symptoms=' + encodeURIComponent(symptoms));
        if (!res.ok) return;
        const data = await res.json();

        document.getElementById('triage-condition').textContent = data.condition;
        document.getElementById('triage-specialist').textContent = 'Specialist: ' + data.specialist;
        
        const urgBadge = document.getElementById('triage-urgency-badge');
        urgBadge.textContent = data.urgency;
        urgBadge.className = 'badge ' + (data.urgency === 'CRITICAL' ? 'badge-critical' : 'badge-blue');

        const confBadge = document.getElementById('triage-confidence-badge');
        confBadge.textContent = data.confidence.toFixed(1) + '% Confidence';

        document.getElementById('triage-guidance').textContent = data.guidance || 'Seek immediate medical attention or call emergency services.';

        // Render Local Feature Attributions
        const attrList = document.getElementById('triage-attributions-list');
        attrList.innerHTML = '';

        if (!data.attributions || data.attributions.length === 0) {
            attrList.innerHTML = '<div style="color:#94a3b8; font-size:0.85rem;">No strong symptom associations found.</div>';
        } else {
            const maxContrib = Math.max(...data.attributions.map(a => Math.abs(a.contributionScore)), 1.0);
            data.attributions.forEach(attr => {
                const widthPct = Math.min(100, Math.round((Math.abs(attr.contributionScore) / maxContrib) * 100));
                const item = `
                    <div class="attribution-row">
                        <div class="attribution-meta">
                            <span><strong>${attr.feature}</strong> (weight: ${attr.weight.toFixed(2)})</span>
                            <span style="color: #38bdf8;">+${attr.contributionScore.toFixed(2)} score</span>
                        </div>
                        <div class="attribution-bar-bg">
                            <div class="attribution-bar-fill" style="width: ${widthPct}%;"></div>
                        </div>
                    </div>
                `;
                attrList.innerHTML += item;
            });
        }
    } catch (err) {
        console.error('Error running triage prediction:', err);
    } finally {
        if (btn) btn.textContent = 'Run Explainable Triage';
    }
}

// -------------------------------------------------------------
// Manual Emergency Dispatch Logic
// -------------------------------------------------------------
document.getElementById('dispatch-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const type = document.getElementById('dispatch-type').value;
    const sev = document.getElementById('dispatch-sev').value;
    const loc = document.getElementById('dispatch-loc').value;
    
    if (!type || !sev || !loc) return;
    
    const payload = { type, severity: sev, location: loc, password: 'admin123' };
    const btn = document.getElementById('dispatch-btn');
    btn.textContent = 'Dispatching...';
    btn.disabled = true;

    try {
        const response = await fetch('/api/dispatch', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const result = await response.json();
        if (!response.ok) {
            alert(result.error || "Dispatch failed");
            return;
        }
        console.log('Dispatched:', result);
        document.getElementById('dispatch-form').reset();
        fetchAI2Data();
    } catch (err) {
        console.error('Dispatch error:', err);
    } finally {
        setTimeout(() => {
            btn.textContent = 'Dispatch Emergency';
            btn.disabled = false;
        }, 1000);
    }
});
