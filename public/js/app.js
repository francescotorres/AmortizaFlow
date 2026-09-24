/**
 * AmortizaFlow - Web Application Controller (Render.com Version)
 */

(function () {
    'use strict';

    // Application State
    const state = {
        summary: null,
        installments: [],
        currentTab: 'dashboard',
        currentFilter: 'ALL',
        simMode: 'REDUCE_TERM',
        simAmount: 1000,
        simResult: null,
        theme: localStorage.getItem('amortizaflow_theme') || 'light'
    };

    // Currency Formatter
    const currencyFormatter = new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    });

    function formatBRL(value) {
        if (value === undefined || value === null || isNaN(value)) return 'R$ 0,00';
        return currencyFormatter.format(value);
    }

    // Toast Notification Utility
    function showToast(message, type = 'success') {
        const container = document.getElementById('toast-container');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <span class="material-symbols-rounded">${type === 'success' ? 'check_circle' : 'info'}</span>
            <span>${message}</span>
        `;

        container.appendChild(toast);
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3200);
    }

    // Theme Management
    function applyTheme(theme) {
        state.theme = theme;
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('amortizaflow_theme', theme);

        const btn = document.getElementById('btn-theme-toggle');
        if (btn) {
            btn.innerHTML = `<span class="material-symbols-rounded">${theme === 'dark' ? 'light_mode' : 'dark_mode'}</span>`;
        }

        // Redraw canvas charts with updated theme colors
        if (state.summary && state.installments.length > 0) {
            drawDonutChart(state.summary);
            drawCurveChart(state.installments);
        }
    }

    // Navigation System
    window.navigateToTab = function (tabName) {
        state.currentTab = tabName;

        // Update nav tabs
        document.querySelectorAll('.nav-tab, .mobile-nav-item').forEach(el => {
            el.classList.toggle('active', el.getAttribute('data-tab') === tabName);
        });

        // Update view sections
        document.querySelectorAll('.view-section').forEach(el => {
            el.classList.toggle('active', el.id === `view-${tabName}`);
        });

        // Trigger specific tab renders
        if (tabName === 'dashboard') {
            if (state.summary && state.installments.length > 0) {
                drawDonutChart(state.summary);
                drawCurveChart(state.installments);
            }
        } else if (tabName === 'installments') {
            renderInstallments();
        } else if (tabName === 'simulator') {
            runSimulation();
        } else if (tabName === 'reports') {
            fetchReports();
        }
    };

    // ======================== API INTERACTIONS ========================

    async function fetchAllData() {
        try {
            const [sumRes, instRes] = await Promise.all([
                fetch('/api/summary'),
                fetch('/api/installments')
            ]);

            state.summary = await sumRes.json();
            const instData = await instRes.json();
            state.installments = instData.installments || [];

            renderDashboard();
            renderInstallments();
        } catch (err) {
            console.error('Erro ao carregar dados:', err);
            showToast('Erro ao sincronizar com o servidor.', 'info');
        }
    }

    async function toggleInstallment(number) {
        try {
            const res = await fetch(`/api/installments/${number}/toggle`, { method: 'POST' });
            const data = await res.json();
            if (data.success) {
                state.summary = data.summary;
                const idx = state.installments.findIndex(i => i.parcelNumber === number);
                if (idx !== -1) state.installments[idx] = data.installment;

                renderDashboard();
                renderInstallments();
                showToast(`Status da parcela #${number} alterado!`);
            }
        } catch (err) {
            console.error(err);
            showToast('Falha ao atualizar parcela.', 'info');
        }
    }

    async function updateInstallmentDetails(number, actualPaid, paymentDate, notes) {
        try {
            const res = await fetch(`/api/installments/${number}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ actualPaidAmount: actualPaid, paymentDate, notes })
            });
            const data = await res.json();
            if (data.success) {
                state.summary = data.summary;
                const idx = state.installments.findIndex(i => i.parcelNumber === number);
                if (idx !== -1) state.installments[idx] = data.installment;

                renderDashboard();
                renderInstallments();
                showToast(`Parcela #${number} salva com sucesso!`);
            }
        } catch (err) {
            console.error(err);
            showToast('Erro ao salvar edição.', 'info');
        }
    }

    async function runSimulation() {
        const amount = parseFloat(document.getElementById('sim-input-amount').value) || 0;
        state.simAmount = amount;

        if (amount <= 0) {
            document.getElementById('sim-result-card').style.display = 'none';
            return;
        }

        try {
            const res = await fetch('/api/simulate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ amount: state.simAmount, type: state.simMode })
            });
            const result = await res.json();
            state.simResult = result;
            renderSimulationResult(result);
        } catch (err) {
            console.error(err);
        }
    }

    async function applySimulation() {
        if (!state.simResult || !state.simResult.eliminatedInstallments || state.simResult.eliminatedInstallments.length === 0) {
            showToast('Nenhuma parcela selecionada para amortizar.', 'info');
            return;
        }

        try {
            const res = await fetch('/api/simulate/apply', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ eliminatedInstallments: state.simResult.eliminatedInstallments })
            });
            const data = await res.json();
            if (data.success) {
                showToast(`Amortização de ${data.appliedCount} parcelas aplicada no contrato!`);
                await fetchAllData();
                window.navigateToTab('dashboard');
            }
        } catch (err) {
            console.error(err);
            showToast('Erro ao aplicar amortização.', 'info');
        }
    }

    async function fetchReports() {
        try {
            const res = await fetch('/api/reports');
            const reports = await res.json();
            renderReportsTable(reports);
        } catch (err) {
            console.error(err);
        }
    }

    async function resetSpreadsheet() {
        if (!confirm('Deseja realmente restaurar todas as 60 parcelas e o histórico exatamente para a planilha de amortização original?')) {
            return;
        }

        try {
            const res = await fetch('/api/reset', { method: 'POST' });
            const data = await res.json();
            if (data.success) {
                showToast(data.message);
                await fetchAllData();
                if (state.currentTab === 'reports') fetchReports();
            }
        } catch (err) {
            console.error(err);
            showToast('Erro ao restaurar base.', 'info');
        }
    }

    // ======================== RENDER FUNCTIONS ========================

    function renderDashboard() {
        const s = state.summary;
        if (!s) return;

        // Next Due
        const nextDue = s.nextDueInstallment;
        const banner = document.getElementById('next-due-banner');
        if (nextDue) {
            banner.style.display = 'flex';
            document.getElementById('next-due-label').innerText = `Próxima Parcela (#${nextDue.parcelNumber})`;
            document.getElementById('next-due-value').innerText = formatBRL(nextDue.nominalAmount);
            document.getElementById('next-due-date').innerText = `Vencimento em ${nextDue.dueDate}`;

            const btnPay = document.getElementById('btn-pay-next');
            btnPay.onclick = () => toggleInstallment(nextDue.parcelNumber);
        } else {
            banner.style.display = 'none';
        }

        // KPIs
        document.getElementById('kpi-remaining-balance').innerText = formatBRL(s.remainingNominalBalance);
        document.getElementById('kpi-balance-badge').innerText = `${s.remainingInstallmentsCount} a pagar`;
        document.getElementById('kpi-amortized-paid').innerText = `Amortizado: ${formatBRL(s.amortizedPrincipalPaid)}`;

        document.getElementById('kpi-total-paid').innerText = formatBRL(s.totalPaidAmount);
        document.getElementById('kpi-paid-badge').innerText = `${s.paidInstallmentsCount} quitadas`;
        document.getElementById('kpi-paid-sub').innerText = `${s.paidInstallmentsCount} de ${s.totalInstallments} parcelas quitadas`;

        document.getElementById('kpi-saved-interest').innerText = formatBRL(s.totalSavedInterest);

        document.getElementById('kpi-months-advanced').innerText = `${s.monthsAdvanced} Meses`;
        document.getElementById('kpi-advanced-badge').innerText = `-${s.monthsAdvanced} Meses`;

        // Canvas Charts
        drawDonutChart(s);
        drawCurveChart(state.installments);
    }

    function drawDonutChart(s) {
        const canvas = document.getElementById('chart-donut');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        const size = canvas.width;
        const center = size / 2;
        const radius = (size / 2) - 16;
        const strokeWidth = 18;

        ctx.clearRect(0, 0, size, size);

        // Track (pending)
        const isDark = state.theme === 'dark';
        const trackColor = isDark ? '#1E2D2A' : '#E8ECE9';
        const greenColor = '#10B981';
        const blueColor = '#3B82F6';

        ctx.beginPath();
        ctx.arc(center, center, radius, 0, 2 * Math.PI);
        ctx.lineWidth = strokeWidth;
        ctx.strokeStyle = trackColor;
        ctx.stroke();

        const total = s.totalInstallments || 60;
        const anticipated = s.anticipatedInstallmentsCount || 0;
        const regularPaid = Math.max(0, s.paidInstallmentsCount - anticipated);

        const regularAngle = (regularPaid / total) * 2 * Math.PI;
        const anticipatedAngle = (anticipated / total) * 2 * Math.PI;
        const startAngle = -Math.PI / 2;

        // Regular Paid Arc
        if (regularAngle > 0) {
            ctx.beginPath();
            ctx.arc(center, center, radius, startAngle, startAngle + regularAngle);
            ctx.lineWidth = strokeWidth;
            ctx.strokeStyle = greenColor;
            ctx.lineCap = 'round';
            ctx.stroke();
        }

        // Anticipated Paid Arc
        if (anticipatedAngle > 0) {
            ctx.beginPath();
            ctx.arc(center, center, radius, startAngle + regularAngle, startAngle + regularAngle + anticipatedAngle);
            ctx.lineWidth = strokeWidth;
            ctx.strokeStyle = blueColor;
            ctx.lineCap = 'round';
            ctx.stroke();
        }

        // Center Text
        document.getElementById('donut-percentage').innerText = `${s.progressPercentage}%`;
        document.getElementById('legend-regular-count').innerText = `${regularPaid} parcelas`;
        document.getElementById('legend-anticipated-count').innerText = `${anticipated} parcelas`;
        document.getElementById('legend-pending-count').innerText = `${s.remainingInstallmentsCount} parcelas`;
    }

    function drawCurveChart(installments) {
        const canvas = document.getElementById('chart-curve');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');

        // Set high DPI scaling
        const rect = canvas.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 1;
        canvas.width = (rect.width || 600) * dpr;
        canvas.height = (rect.height || 200) * dpr;
        ctx.scale(dpr, dpr);

        const w = rect.width || 600;
        const h = rect.height || 200;
        const padBottom = 25;
        const chartH = h - padBottom;

        ctx.clearRect(0, 0, w, h);

        const isDark = state.theme === 'dark';
        const primaryColor = isDark ? '#34D399' : '#006C4C';
        const gridColor = isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.06)';

        // Horizontal Grid lines
        for (let i = 0; i <= 4; i++) {
            const y = chartH * (i / 4);
            ctx.beginPath();
            ctx.moveTo(0, y);
            ctx.lineTo(w, y);
            ctx.strokeStyle = gridColor;
            ctx.lineWidth = 1;
            ctx.stroke();
        }

        if (installments.length === 0) return;

        // Build remaining balance curve
        let runningBalance = 59122.80;
        const points = [];

        installments.forEach((item, idx) => {
            if (item.isPaid) {
                runningBalance -= (item.actualPaidAmount || item.nominalAmount);
            } else {
                runningBalance -= item.theoreticalAmortization;
            }
            const x = (idx / (installments.length - 1)) * w;
            const normY = Math.max(0, Math.min(1, runningBalance / 59122.80));
            const y = chartH * (1 - normY);
            points.push({ x, y });
        });

        // Area Fill
        ctx.beginPath();
        ctx.moveTo(points[0].x, chartH);
        ctx.lineTo(points[0].x, points[0].y);
        for (let i = 1; i < points.length; i++) {
            ctx.lineTo(points[i].x, points[i].y);
        }
        ctx.lineTo(points[points.length - 1].x, chartH);
        ctx.closePath();

        const grad = ctx.createLinearGradient(0, 0, 0, chartH);
        grad.addColorStop(0, isDark ? 'rgba(52, 211, 153, 0.35)' : 'rgba(0, 108, 76, 0.3)');
        grad.addColorStop(1, 'rgba(0, 108, 76, 0.0)');
        ctx.fillStyle = grad;
        ctx.fill();

        // Stroke line
        ctx.beginPath();
        ctx.moveTo(points[0].x, points[0].y);
        for (let i = 1; i < points.length; i++) {
            ctx.lineTo(points[i].x, points[i].y);
        }
        ctx.strokeStyle = primaryColor;
        ctx.lineWidth = 3;
        ctx.lineCap = 'round';
        ctx.stroke();

        // Current installment marker (around #5)
        const curIdx = Math.min(5, points.length - 1);
        const curPoint = points[curIdx];
        if (curPoint) {
            ctx.beginPath();
            ctx.arc(curPoint.x, curPoint.y, 6, 0, 2 * Math.PI);
            ctx.fillStyle = primaryColor;
            ctx.fill();

            ctx.beginPath();
            ctx.arc(curPoint.x, curPoint.y, 3, 0, 2 * Math.PI);
            ctx.fillStyle = '#FFFFFF';
            ctx.fill();
        }

        const labelEl = document.getElementById('curve-current-balance-label');
        if (labelEl && state.summary) {
            labelEl.innerText = `Atual: ${formatBRL(state.summary.remainingNominalBalance)}`;
        }
    }

    function renderInstallments() {
        const container = document.getElementById('installments-list');
        if (!container) return;

        const list = state.installments;
        const paidCount = list.filter(i => i.isPaid).length;
        const pendingCount = list.filter(i => !i.isPaid).length;
        const anticipatedCount = list.filter(i => i.isAnticipated).length;

        document.getElementById('count-all').innerText = list.length;
        document.getElementById('count-paid').innerText = paidCount;
        document.getElementById('count-pending').innerText = pendingCount;
        document.getElementById('count-anticipated').innerText = anticipatedCount;

        let filtered = list;
        if (state.currentFilter === 'PAID') filtered = list.filter(i => i.isPaid);
        else if (state.currentFilter === 'PENDING') filtered = list.filter(i => !i.isPaid);
        else if (state.currentFilter === 'ANTICIPATED') filtered = list.filter(i => i.isAnticipated);

        container.innerHTML = '';

        filtered.forEach(item => {
            const card = document.createElement('div');
            const isAnticipated = item.isAnticipated;
            const isPaid = item.isPaid;

            card.className = `installment-card ${isAnticipated ? 'is-anticipated' : (isPaid ? 'is-paid' : '')}`;

            const statusClass = isAnticipated ? 'blue' : (isPaid ? 'green' : '');
            const statusText = isAnticipated ? 'Amortizada (Fim)' : (isPaid ? 'Paga' : 'Pendente');

            card.innerHTML = `
                <div class="card-left">
                    <div class="parcel-circle">#${item.parcelNumber}</div>
                    <div class="parcel-details">
                        <h4>
                            <span>${item.dueDate}</span>
                            <span class="badge ${statusClass}">${statusText}</span>
                        </h4>
                        <p class="parcel-meta">
                            Amortização: <strong>${formatBRL(item.theoreticalAmortization)}</strong> | Juros: ${formatBRL(item.interestAmount)}
                            ${item.actualPaidAmount ? ` | Pago: <strong>${formatBRL(item.actualPaidAmount)}</strong>` : ''}
                        </p>
                        ${item.savedInterest > 0 ? `
                            <span class="savings-tag">
                                <span class="material-symbols-rounded" style="font-size:14px;">savings</span>
                                Economia: +${formatBRL(item.savedInterest)}
                            </span>
                        ` : ''}
                    </div>
                </div>
                <div class="card-right">
                    <button class="icon-btn btn-edit-parcel" title="Editar dados da parcela" data-parcel="${item.parcelNumber}">
                        <span class="material-symbols-rounded">edit</span>
                    </button>
                    <button class="btn-toggle-paid ${isAnticipated ? 'anticipated' : (isPaid ? 'paid' : '')}" title="Alternar status" data-parcel="${item.parcelNumber}">
                        <span class="material-symbols-rounded">${isPaid ? 'check' : 'hourglass_empty'}</span>
                    </button>
                </div>
            `;

            // Button toggle
            card.querySelector('.btn-toggle-paid').addEventListener('click', (e) => {
                e.stopPropagation();
                toggleInstallment(item.parcelNumber);
            });

            // Button edit
            card.querySelector('.btn-edit-parcel').addEventListener('click', (e) => {
                e.stopPropagation();
                openEditModal(item);
            });

            container.appendChild(card);
        });
    }

    function openEditModal(item) {
        const dialog = document.getElementById('edit-dialog');
        document.getElementById('dialog-title').innerText = `Editar Parcela #${item.parcelNumber}`;
        document.getElementById('edit-parcel-number').value = item.parcelNumber;
        document.getElementById('edit-due-date').value = item.dueDate;
        document.getElementById('edit-paid-amount').value = item.actualPaidAmount || item.nominalAmount;
        document.getElementById('edit-payment-date').value = item.paymentDate || item.dueDate;
        document.getElementById('edit-notes').value = item.notes || '';

        dialog.showModal();
    }

    function renderSimulationResult(res) {
        const card = document.getElementById('sim-result-card');
        card.style.display = 'block';

        const termGroup = document.getElementById('res-term-group');
        const paymentGroup = document.getElementById('res-payment-group');
        const btnApply = document.getElementById('btn-apply-sim');

        if (res.type === 'REDUCE_TERM') {
            termGroup.style.display = 'block';
            paymentGroup.style.display = 'none';

            document.getElementById('sim-badge').innerText = 'Redução de Prazo';
            document.getElementById('sim-res-eliminated').innerText = res.eliminatedInstallments.length > 0
                ? `${res.monthsShortened} parcelas (${res.eliminatedInstallments.map(n => `#${n}`).join(', ')})`
                : 'Nenhuma parcela coberta com este valor';

            document.getElementById('sim-res-saved').innerText = formatBRL(res.totalSavedInterest);
            document.getElementById('sim-res-date').innerText = res.newProjectedEndDate;

            document.getElementById('sim-explainer-text').innerText =
                `Ao amortizar ${formatBRL(res.totalPaidInSimulation)} de trás para frente, você remove integralmente ${formatBRL(res.totalSavedInterest)} de juros futuros!`;

            btnApply.style.display = res.eliminatedInstallments.length > 0 ? 'inline-flex' : 'none';
        } else {
            termGroup.style.display = 'none';
            paymentGroup.style.display = 'block';

            document.getElementById('sim-badge').innerText = 'Redução de Prestação';
            document.getElementById('sim-res-new-payment').innerText = formatBRL(res.newMonthlyInstallment);
            document.getElementById('sim-res-saved').innerText = formatBRL(res.totalSavedInterest);
            document.getElementById('sim-res-date').innerText = res.newProjectedEndDate;

            document.getElementById('sim-explainer-text').innerText =
                `Ao abater ${formatBRL(res.simulatedAmount)} no contrato, sua parcela mensal diminui para ${formatBRL(res.newMonthlyInstallment)} mantendo o mesmo prazo.`;

            btnApply.style.display = 'none';
        }
    }

    function renderReportsTable(reports) {
        const tbody = document.getElementById('report-tbody');
        if (!tbody) return;
        tbody.innerHTML = '';

        reports.forEach(r => {
            const tr = document.createElement('tr');
            const statusBadge = r.isAnticipated
                ? '<span class="badge blue">Amortizada</span>'
                : (r.isPaid ? '<span class="badge green">Quitada</span>' : '<span class="badge">Pendente</span>');

            tr.innerHTML = `
                <td><strong>#${r.parcelNumber}</strong></td>
                <td>${r.monthYear}</td>
                <td>${statusBadge}</td>
                <td>${formatBRL(r.initialBalance)}</td>
                <td style="color:var(--primary); font-weight:600;">${formatBRL(r.amortizationAmount)}</td>
                <td>${formatBRL(r.interestPaid)}</td>
                <td><strong>${formatBRL(r.paymentAmount)}</strong></td>
                <td style="color:var(--gold); font-weight:700;">${r.savedInterest > 0 ? `+${formatBRL(r.savedInterest)}` : '-'}</td>
                <td><strong>${formatBRL(r.finalBalance)}</strong></td>
            `;
            tbody.appendChild(tr);
        });
    }

    function shareReport() {
        const s = state.summary;
        if (!s) return;

        const text = `📊 RELATÓRIO DO FINANCIAMENTO - AMORTIZAFLOW\n` +
            `====================================\n` +
            `Saldo Devedor Atual: ${formatBRL(s.remainingNominalBalance)}\n` +
            `Total Pago: ${formatBRL(s.totalPaidAmount)} (${s.paidInstallmentsCount} parcelas)\n` +
            `Juros Totais Poupados: ${formatBRL(s.totalSavedInterest)}\n` +
            `Prazo Adiantado: ${s.monthsAdvanced} meses\n` +
            `Próxima Parcela: #${s.nextDueInstallment ? s.nextDueInstallment.parcelNumber : '-'}\n` +
            `====================================\n` +
            `Acesse seu app AmortizaFlow em produção!`;

        if (navigator.share) {
            navigator.share({
                title: 'Relatório AmortizaFlow',
                text: text
            }).catch(() => {});
        } else {
            navigator.clipboard.writeText(text);
            showToast('Relatório copiado para a Área de Transferência!');
        }
    }

    // ======================== INITIALIZATION ========================

    function bindEvents() {
        // Nav tabs click
        document.querySelectorAll('.nav-tab, .mobile-nav-item').forEach(btn => {
            btn.addEventListener('click', () => {
                const tab = btn.getAttribute('data-tab');
                window.navigateToTab(tab);
            });
        });

        // Theme toggle
        document.getElementById('btn-theme-toggle').addEventListener('click', () => {
            const nextTheme = state.theme === 'dark' ? 'light' : 'dark';
            applyTheme(nextTheme);
        });

        // Filter chips
        document.querySelectorAll('.filter-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
                chip.classList.add('active');
                state.currentFilter = chip.getAttribute('data-filter');
                renderInstallments();
            });
        });

        // Simulator mode switcher
        document.querySelectorAll('.sim-mode-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.sim-mode-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                state.simMode = btn.getAttribute('data-mode');
                runSimulation();
            });
        });

        // Simulator input
        const simInput = document.getElementById('sim-input-amount');
        simInput.addEventListener('input', runSimulation);

        // Simulator quick chips
        document.querySelectorAll('.chip-val').forEach(chip => {
            chip.addEventListener('click', () => {
                document.querySelectorAll('.chip-val').forEach(c => c.classList.remove('active'));
                chip.classList.add('active');
                simInput.value = chip.getAttribute('data-val');
                runSimulation();
            });
        });

        // Apply simulation button
        document.getElementById('btn-apply-sim').addEventListener('click', applySimulation);

        // Reports actions
        document.getElementById('btn-reset-data').addEventListener('click', resetSpreadsheet);
        document.getElementById('btn-share-report').addEventListener('click', shareReport);

        // Modal Edit form submission
        document.getElementById('form-edit-installment').addEventListener('submit', (e) => {
            e.preventDefault();
            const number = parseInt(document.getElementById('edit-parcel-number').value, 10);
            const paid = parseFloat(document.getElementById('edit-paid-amount').value);
            const date = document.getElementById('edit-payment-date').value;
            const notes = document.getElementById('edit-notes').value;

            document.getElementById('edit-dialog').close();
            updateInstallmentDetails(number, paid, date, notes);
        });

        // Resize redraw for charts
        window.addEventListener('resize', () => {
            if (state.summary && state.installments.length > 0) {
                drawCurveChart(state.installments);
            }
        });
    }

    // PWA Service Worker Registration
    if ('serviceWorker' in navigator) {
        window.addEventListener('load', () => {
            navigator.serviceWorker.register('/sw.js').catch(err => console.log('SW registration error:', err));
        });
    }

    // Start
    document.addEventListener('DOMContentLoaded', () => {
        applyTheme(state.theme);
        bindEvents();
        fetchAllData();
    });

})();
