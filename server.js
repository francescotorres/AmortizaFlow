require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const db = require('./db/turso');

const app = express();
const PORT = process.env.PORT || 10000;

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Initialize database schema and seeds on startup
let isDbReady = false;
let dbMode = 'initializing';

(async () => {
    try {
        const result = await db.initDatabase();
        isDbReady = true;
        dbMode = result?.mode || db.getDatabaseMode();
        console.log(`✅ [Database] Banco de dados operacional no modo: ${dbMode}`);
    } catch (err) {
        console.error('❌ [Database] Falha crítica ao inicializar o banco de dados:', err);
    }
})();

// Middleware to ensure DB is initialized before processing requests
app.use(async (req, res, next) => {
    if (req.path.startsWith('/api/') && !isDbReady) {
        try {
            const result = await db.initDatabase();
            isDbReady = true;
            dbMode = result?.mode || db.getDatabaseMode();
        } catch (err) {
            return res.status(503).json({ error: 'Banco de dados inicializando... Tente novamente em alguns segundos.' });
        }
    }
    next();
});

// ======================== API ROUTES ========================

// 1. Health check
app.get('/api/health', (req, res) => {
    res.json({
        status: isDbReady ? 'ok' : 'initializing',
        app: 'AmortizaFlow Web',
        databaseMode: db.getDatabaseMode ? db.getDatabaseMode() : dbMode,
        isDbReady,
        version: '1.2.0',
        timestamp: new Date().toISOString()
    });
});

// 2. Summary (Executive KPIs)
app.get('/api/summary', async (req, res) => {
    try {
        const summary = await db.calculateSummary();
        res.json(summary);
    } catch (err) {
        console.error('Erro em /api/summary:', err);
        res.status(500).json({ error: 'Erro ao calcular resumo financeiro', details: err.message });
    }
});

// 3. Installments list
app.get('/api/installments', async (req, res) => {
    try {
        const statusFilter = req.query.status; // 'PAID', 'PENDING', 'ANTICIPATED' or undefined
        const installments = await db.getInstallments(statusFilter);

        const contractRes = await db.client.execute('SELECT * FROM contracts WHERE id = "main_contract"');
        const contractRow = contractRes.rows[0] || {};

        res.json({
            contract: {
                title: contractRow.title || 'Financiamento Imobiliário / Veículo',
                totalInstallments: Number(contractRow.total_installments || 60),
                nominalInstallment: Number(contractRow.nominal_installment || 985.38),
                totalContractAmount: Number(contractRow.total_contract_amount || 59122.80),
                monthlyInterestRate: Number(contractRow.monthly_interest_rate || 0.0165),
                startDate: contractRow.start_date || '14/05/2026',
                originalEndDate: contractRow.original_end_date || '14/04/2031'
            },
            installments
        });
    } catch (err) {
        console.error('Erro em /api/installments:', err);
        res.status(500).json({ error: 'Erro ao consultar parcelas no Turso', details: err.message });
    }
});

// 4. Toggle installment paid state
app.post('/api/installments/:number/toggle', async (req, res) => {
    try {
        const parcelNumber = parseInt(req.params.number, 10);
        const updatedItem = await db.toggleInstallmentPayment(parcelNumber);

        if (!updatedItem) {
            return res.status(404).json({ error: 'Parcela não encontrada' });
        }

        const summary = await db.calculateSummary();
        res.json({
            success: true,
            installment: updatedItem,
            summary
        });
    } catch (err) {
        console.error('Erro em /api/installments/:number/toggle:', err);
        res.status(500).json({ error: 'Erro ao alternar status da parcela', details: err.message });
    }
});

// 5. Update installment details
app.put('/api/installments/:number', async (req, res) => {
    try {
        const parcelNumber = parseInt(req.params.number, 10);
        const { actualPaidAmount, paymentDate, notes, isPaid } = req.body;

        const updatedItem = await db.updateInstallmentDetails(parcelNumber, {
            actualPaidAmount,
            paymentDate,
            notes,
            isPaid
        });

        if (!updatedItem) {
            return res.status(404).json({ error: 'Parcela não encontrada' });
        }

        const summary = await db.calculateSummary();
        res.json({
            success: true,
            installment: updatedItem,
            summary
        });
    } catch (err) {
        console.error('Erro em PUT /api/installments/:number:', err);
        res.status(500).json({ error: 'Erro ao atualizar dados da parcela', details: err.message });
    }
});

// 6. Simulate amortization
app.post('/api/simulate', async (req, res) => {
    try {
        const { amount, type } = req.body;
        const simAmount = parseFloat(amount) || 0;
        const simType = type || 'REDUCE_TERM';

        const pending = await db.getInstallments('PENDING');

        if (simType === 'REDUCE_TERM') {
            // Sort pending from end to front
            const pendingFromEnd = [...pending].sort((a, b) => b.parcelNumber - a.parcelNumber);
            let budget = simAmount;
            const eliminated = [];
            let totalSaved = 0;
            let totalCost = 0;

            for (const item of pendingFromEnd) {
                const cost = item.theoreticalAmortization;
                if (budget >= cost) {
                    budget -= cost;
                    eliminated.push(item.parcelNumber);
                    totalSaved += (item.nominalAmount - cost);
                    totalCost += cost;
                } else {
                    break;
                }
            }

            const remainingPending = pendingFromEnd.filter(i => !eliminated.includes(i.parcelNumber));
            const lastActive = remainingPending.sort((a, b) => b.parcelNumber - a.parcelNumber)[0];
            const projectedEnd = lastActive ? lastActive.dueDate : 'Totalmente Quitado!';

            return res.json({
                simulatedAmount: simAmount,
                type: simType,
                eliminatedInstallments: eliminated,
                totalSavedInterest: parseFloat(totalSaved.toFixed(2)),
                totalPaidInSimulation: parseFloat(totalCost.toFixed(2)),
                monthsShortened: eliminated.length,
                newProjectedEndDate: projectedEnd,
                newMonthlyInstallment: null
            });
        } else {
            const count = pending.length;
            const reductionPerParcel = count > 0 ? (simAmount / count) : 0;
            const newInstallment = Math.max(0, 985.38 - reductionPerParcel);

            return res.json({
                simulatedAmount: simAmount,
                type: simType,
                eliminatedInstallments: [],
                totalSavedInterest: parseFloat((simAmount * 0.45).toFixed(2)),
                totalPaidInSimulation: simAmount,
                monthsShortened: 0,
                newProjectedEndDate: pending.length > 0 ? pending[pending.length - 1].dueDate : '14/04/2031',
                newMonthlyInstallment: parseFloat(newInstallment.toFixed(2))
            });
        }
    } catch (err) {
        console.error('Erro em /api/simulate:', err);
        res.status(500).json({ error: 'Erro ao executar simulação', details: err.message });
    }
});

// 7. Apply simulation to actual contract in Turso
app.post('/api/simulate/apply', async (req, res) => {
    try {
        const { eliminatedInstallments, amount, type } = req.body;
        if (!Array.isArray(eliminatedInstallments) || eliminatedInstallments.length === 0) {
            return res.status(400).json({ error: 'Nenhuma parcela especificada para quitação' });
        }

        const result = await db.applyExtraordinaryAmortization(eliminatedInstallments, amount, type);
        const summary = await db.calculateSummary();

        res.json({
            success: true,
            appliedCount: result.appliedCount,
            savedInterest: result.savedInterest,
            summary
        });
    } catch (err) {
        console.error('Erro em /api/simulate/apply:', err);
        res.status(500).json({ error: 'Erro ao aplicar amortização', details: err.message });
    }
});

// 8. Monthly Reports
app.get('/api/reports', async (req, res) => {
    try {
        const reports = await db.getMonthlyReports();
        res.json(reports);
    } catch (err) {
        console.error('Erro em /api/reports:', err);
        res.status(500).json({ error: 'Erro ao gerar relatórios mensais', details: err.message });
    }
});

// 9. Reset to official spreadsheet data
app.post('/api/reset', async (req, res) => {
    try {
        const summary = await db.resetToOfficialData();
        res.json({
            success: true,
            message: 'Dados oficiais da planilha restaurados com sucesso no banco Turso!',
            summary
        });
    } catch (err) {
        console.error('Erro em /api/reset:', err);
        res.status(500).json({ error: 'Erro ao restaurar base de dados oficial', details: err.message });
    }
});

// 10. CSV Export
app.get('/api/export/csv', async (req, res) => {
    try {
        const installments = await db.getInstallments();
        let csv = "PARCELA;VENCIMENTO;STATUS;VALOR_NOMINAL;AMORTIZACAO;JUROS;VALOR_PAGO;DATA_PAGAMENTO;ECONOMIA_JUROS;NOTAS\n";

        for (const i of installments) {
            const status = i.isAnticipated ? "AMORTIZADA_FIM" : (i.isPaid ? "PAGA" : "PENDENTE");
            const paidVal = i.actualPaidAmount !== null ? i.actualPaidAmount.toFixed(2) : "";
            const payDate = i.paymentDate || "";
            const saved = i.savedInterest ? i.savedInterest.toFixed(2) : "0.00";
            const notes = (i.notes || "").replace(/;/g, ',');

            csv += `${i.parcelNumber};${i.dueDate};${status};${i.nominalAmount.toFixed(2)};${i.theoreticalAmortization.toFixed(2)};${i.interestAmount.toFixed(2)};${paidVal};${payDate};${saved};"${notes}"\n`;
        }

        res.setHeader('Content-Type', 'text/csv; charset=utf-8');
        res.setHeader('Content-Disposition', 'attachment; filename="AmortizaFlow_Extrato.csv"');
        res.send("\uFEFF" + csv); // UTF-8 BOM
    } catch (err) {
        console.error('Erro em /api/export/csv:', err);
        res.status(500).json({ error: 'Erro ao exportar CSV', details: err.message });
    }
});

// Catch-all: serve index.html for SPA routing
app.get('*', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`=======================================================`);
    console.log(`🚀 AmortizaFlow Web Service rodando na porta ${PORT}`);
    console.log(`💾 Banco de Dados: Turso Cloud LibSQL`);
    console.log(`🔗 Local: http://localhost:${PORT}`);
    console.log(`🌐 Ambiente: ${process.env.NODE_ENV || 'development'}`);
    console.log(`=======================================================`);
});
