const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 10000;

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

const DATA_FILE = path.join(__dirname, 'data', 'financing_data.json');
const BACKUP_FILE = path.join(__dirname, 'data', 'financing_data_backup.json');

// Ensure backup exists for reset functionality
function ensureDataFiles() {
    if (!fs.existsSync(DATA_FILE)) {
        require('./scripts/generate_data.js');
    }
    if (!fs.existsSync(BACKUP_FILE)) {
        fs.copyFileSync(DATA_FILE, BACKUP_FILE);
    }
}
ensureDataFiles();

function readData() {
    const raw = fs.readFileSync(DATA_FILE, 'utf-8');
    return JSON.parse(raw);
}

function writeData(data) {
    fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2), 'utf-8');
}

function calculateSummary(installments) {
    const totalContract = 59122.80;
    const paidList = installments.filter(i => i.isPaid);
    const pendingList = installments.filter(i => !i.isPaid);

    const totalPaid = paidList.reduce((acc, i) => acc + (i.actualPaidAmount || i.nominalAmount), 0);
    const remainingNominal = pendingList.reduce((acc, i) => acc + i.nominalAmount, 0);
    const totalSavedInterest = paidList.reduce((acc, i) => acc + (i.savedInterest || 0), 0);
    const amortizedPrincipal = paidList.reduce((acc, i) => acc + i.theoreticalAmortization, 0);
    const anticipatedCount = paidList.filter(i => i.isAnticipated).length;
    const nextDue = pendingList.sort((a, b) => a.parcelNumber - b.parcelNumber)[0] || null;
    const progress = installments.length > 0 ? (paidList.length / installments.length) : 0;

    return {
        totalContractAmount: totalContract,
        totalPaidAmount: parseFloat(totalPaid.toFixed(2)),
        remainingNominalBalance: parseFloat(remainingNominal.toFixed(2)),
        amortizedPrincipalPaid: parseFloat(amortizedPrincipal.toFixed(2)),
        totalSavedInterest: parseFloat(totalSavedInterest.toFixed(2)),
        totalInstallments: installments.length,
        paidInstallmentsCount: paidList.length,
        remainingInstallmentsCount: pendingList.length,
        anticipatedInstallmentsCount: anticipatedCount,
        nextDueInstallment: nextDue,
        progressPercentage: parseFloat((progress * 100).toFixed(1)),
        monthsAdvanced: anticipatedCount
    };
}

function generateMonthlyReports(installments) {
    let currentBalance = 59122.80;
    const reports = [];

    for (const item of installments) {
        const initBal = currentBalance;
        const paid = item.actualPaidAmount || item.nominalAmount;
        const amort = item.theoreticalAmortization;
        const interest = item.interestAmount;
        const saved = item.savedInterest || 0;

        if (item.isPaid) {
            currentBalance -= paid;
        } else {
            currentBalance -= amort;
        }
        const finalBal = Math.max(0, currentBalance);

        reports.push({
            monthYear: item.dueDate,
            parcelNumber: item.parcelNumber,
            initialBalance: parseFloat(initBal.toFixed(2)),
            paymentAmount: parseFloat(paid.toFixed(2)),
            amortizationAmount: parseFloat(amort.toFixed(2)),
            interestPaid: parseFloat(interest.toFixed(2)),
            savedInterest: parseFloat(saved.toFixed(2)),
            finalBalance: parseFloat(finalBal.toFixed(2)),
            isPaid: item.isPaid,
            isAnticipated: item.isAnticipated,
            notes: item.notes || ""
        });
    }
    return reports;
}

// ======================== API ROUTES ========================

// 1. Health check
app.get('/api/health', (req, res) => {
    res.json({
        status: 'ok',
        app: 'AmortizaFlow Web',
        version: '1.0.0',
        timestamp: new Date().toISOString()
    });
});

// 2. Summary
app.get('/api/summary', (req, res) => {
    const data = readData();
    const summary = calculateSummary(data.installments);
    res.json(summary);
});

// 3. Installments list
app.get('/api/installments', (req, res) => {
    const data = readData();
    res.json({
        contract: data.contract,
        installments: data.installments
    });
});

// 4. Toggle installment paid state
app.post('/api/installments/:number/toggle', (req, res) => {
    const parcelNumber = parseInt(req.params.number, 10);
    const data = readData();
    const item = data.installments.find(i => i.parcelNumber === parcelNumber);

    if (!item) {
        return res.status(404).json({ error: 'Parcela não encontrada' });
    }

    const newPaidStatus = !item.isPaid;
    const today = new Date();
    const todayStr = `${String(today.getDate()).padStart(2, '0')}/${String(today.getMonth() + 1).padStart(2, '0')}/${today.getFullYear()}`;

    if (newPaidStatus) {
        const paidVal = (item.actualPaidAmount && item.actualPaidAmount > 0)
            ? item.actualPaidAmount
            : item.theoreticalAmortization;
        const saved = Math.max(0, item.nominalAmount - paidVal);

        item.isPaid = true;
        item.actualPaidAmount = parseFloat(paidVal.toFixed(2));
        item.paymentDate = todayStr;
        item.savedInterest = parseFloat(saved.toFixed(2));
    } else {
        item.isPaid = false;
        item.actualPaidAmount = null;
        item.paymentDate = null;
        item.savedInterest = 0.00;
    }

    writeData(data);
    res.json({
        success: true,
        installment: item,
        summary: calculateSummary(data.installments)
    });
});

// 5. Update installment details
app.put('/api/installments/:number', (req, res) => {
    const parcelNumber = parseInt(req.params.number, 10);
    const { actualPaidAmount, paymentDate, notes } = req.body;
    const data = readData();
    const item = data.installments.find(i => i.parcelNumber === parcelNumber);

    if (!item) {
        return res.status(404).json({ error: 'Parcela não encontrada' });
    }

    if (actualPaidAmount !== undefined) {
        const numVal = parseFloat(actualPaidAmount) || item.nominalAmount;
        item.actualPaidAmount = parseFloat(numVal.toFixed(2));
        item.savedInterest = parseFloat(Math.max(0, item.nominalAmount - numVal).toFixed(2));
        item.isPaid = true;
    }
    if (paymentDate) item.paymentDate = paymentDate;
    if (notes !== undefined) item.notes = notes;

    writeData(data);
    res.json({
        success: true,
        installment: item,
        summary: calculateSummary(data.installments)
    });
});

// 6. Simulate amortization
app.post('/api/simulate', (req, res) => {
    const { amount, type } = req.body;
    const simAmount = parseFloat(amount) || 0;
    const simType = type || 'REDUCE_TERM';

    const data = readData();
    const pendingFromEnd = data.installments
        .filter(i => !i.isPaid)
        .sort((a, b) => b.parcelNumber - a.parcelNumber);

    if (simType === 'REDUCE_TERM') {
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
        const pending = data.installments.filter(i => !i.isPaid);
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
});

// 7. Apply simulation to actual contract
app.post('/api/simulate/apply', (req, res) => {
    const { eliminatedInstallments } = req.body;
    if (!Array.isArray(eliminatedInstallments) || eliminatedInstallments.length === 0) {
        return res.status(400).json({ error: 'Nenhuma parcela especificada para quitação' });
    }

    const data = readData();
    const today = new Date();
    const todayStr = `${String(today.getDate()).padStart(2, '0')}/${String(today.getMonth() + 1).padStart(2, '0')}/${today.getFullYear()}`;

    for (const num of eliminatedInstallments) {
        const item = data.installments.find(i => i.parcelNumber === num);
        if (item && !item.isPaid) {
            item.isPaid = true;
            item.isAnticipated = true;
            item.actualPaidAmount = item.theoreticalAmortization;
            item.paymentDate = todayStr;
            item.savedInterest = parseFloat((item.nominalAmount - item.theoreticalAmortization).toFixed(2));
            item.notes = "Amortização extraordinária aplicada pelo app";
        }
    }

    writeData(data);
    res.json({
        success: true,
        appliedCount: eliminatedInstallments.length,
        summary: calculateSummary(data.installments)
    });
});

// 8. Monthly Reports
app.get('/api/reports', (req, res) => {
    const data = readData();
    const reports = generateMonthlyReports(data.installments);
    res.json(reports);
});

// 9. Reset to official spreadsheet data
app.post('/api/reset', (req, res) => {
    if (fs.existsSync(BACKUP_FILE)) {
        fs.copyFileSync(BACKUP_FILE, DATA_FILE);
    } else {
        require('./scripts/generate_data.js');
    }
    const data = readData();
    res.json({
        success: true,
        message: 'Dados oficiais da planilha restaurados com sucesso!',
        summary: calculateSummary(data.installments)
    });
});

// 10. CSV Export
app.get('/api/export/csv', (req, res) => {
    const data = readData();
    let csv = "PARCELA;VENCIMENTO;STATUS;VALOR_NOMINAL;AMORTIZACAO;JUROS;VALOR_PAGO;DATA_PAGAMENTO;ECONOMIA_JUROS;NOTAS\n";

    for (const i of data.installments) {
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
});

// Catch-all: serve index.html for SPA routing
app.get('*', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`===============================================`);
    console.log(`🚀 AmortizaFlow Web Service rodando na porta ${PORT}`);
    console.log(`🔗 Local: http://localhost:${PORT}`);
    console.log(`🌐 Ambiente: ${process.env.NODE_ENV || 'development'}`);
    console.log(`===============================================`);
});
