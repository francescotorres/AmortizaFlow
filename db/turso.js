const { createClient } = require('@libsql/client');
const path = require('path');
const fs = require('fs');
require('dotenv').config();

// On local Windows dev, avoid leaf signature issues if system CA bundle isn't passed
if (process.platform === 'win32' && !process.env.NODE_TLS_REJECT_UNAUTHORIZED && process.env.NODE_ENV !== 'production') {
    process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';
}

const rawUrl = process.env.TURSO_DATABASE_URL || 'file:local.db';
const rawToken = process.env.TURSO_AUTH_TOKEN || '';

// Clean up credentials (strip any accidental whitespaces or line-breaks that cause HTTP 401)
const url = rawUrl.trim();
const authToken = rawToken.replace(/\s+/g, '');

if (!process.env.TURSO_DATABASE_URL) {
    console.warn('[Turso] Warning: TURSO_DATABASE_URL not set in environment. Falling back to local file:local.db');
}

let isUsingLocalFallback = false;
let client = createClient({
    url,
    authToken
});

function switchToLocalFallback(reason) {
    isUsingLocalFallback = true;
    console.warn(`\n⚠️  [Turso Notice] ${reason}`);
    console.warn('🔄 [Turso Fallback] Alternando automaticamente para banco SQLite local em arquivo...');
    console.warn('ℹ️  [Turso Info] O sistema permanecerá online e funcional. Para persistir no Turso Cloud, configure o TURSO_AUTH_TOKEN no Render.');
    
    const dataDir = path.join(__dirname, '..', 'data');
    if (!fs.existsSync(dataDir)) {
        fs.mkdirSync(dataDir, { recursive: true });
    }
    const localDbPath = path.join(dataDir, 'local.db');
    client = createClient({
        url: `file:${localDbPath}`
    });
}

/**
 * Maps database row to standard Installment object matching API/App contracts
 */
function mapRowToInstallment(row) {
    if (!row) return null;
    return {
        parcelNumber: Number(row.parcel_number),
        dueDate: String(row.due_date),
        nominalAmount: Number(row.nominal_amount),
        theoreticalAmortization: Number(row.theoretical_amortization),
        interestAmount: Number(row.interest_amount),
        actualPaidAmount: row.actual_paid_amount !== null && row.actual_paid_amount !== undefined ? Number(row.actual_paid_amount) : null,
        paymentDate: row.payment_date || null,
        isPaid: Boolean(row.is_paid),
        isAnticipated: Boolean(row.is_anticipated),
        savedInterest: Number(row.saved_interest || 0),
        notes: row.notes || ''
    };
}

/**
 * Creates all tables and seeds official data using the current client
 */
async function createTablesAndSeed() {
    // 1. Users Table
    await client.execute(`
        CREATE TABLE IF NOT EXISTS users (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            email TEXT,
            phone TEXT,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        );
    `);

    // 2. Contracts Table
    await client.execute(`
        CREATE TABLE IF NOT EXISTS contracts (
            id TEXT PRIMARY KEY,
            user_id TEXT DEFAULT 'default_user',
            title TEXT NOT NULL,
            contract_number TEXT,
            total_installments INTEGER NOT NULL DEFAULT 60,
            nominal_installment REAL NOT NULL DEFAULT 985.38,
            total_contract_amount REAL NOT NULL DEFAULT 59122.80,
            monthly_interest_rate REAL NOT NULL DEFAULT 0.0165,
            start_date TEXT NOT NULL DEFAULT '14/05/2026',
            original_end_date TEXT NOT NULL DEFAULT '14/04/2031',
            current_projected_end_date TEXT,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        );
    `);

    // 3. Installments Table
    await client.execute(`
        CREATE TABLE IF NOT EXISTS installments (
            parcel_number INTEGER PRIMARY KEY,
            contract_id TEXT DEFAULT 'main_contract',
            due_date TEXT NOT NULL,
            nominal_amount REAL NOT NULL,
            theoretical_amortization REAL NOT NULL,
            interest_amount REAL NOT NULL,
            actual_paid_amount REAL,
            payment_date TEXT,
            is_paid INTEGER NOT NULL DEFAULT 0,
            is_anticipated INTEGER NOT NULL DEFAULT 0,
            saved_interest REAL NOT NULL DEFAULT 0,
            notes TEXT,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        );
    `);

    // 4. Amortization History Table
    await client.execute(`
        CREATE TABLE IF NOT EXISTS amortization_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            contract_id TEXT DEFAULT 'main_contract',
            simulation_type TEXT NOT NULL,
            amount_invested REAL NOT NULL,
            installments_eliminated TEXT NOT NULL,
            months_shortened INTEGER NOT NULL DEFAULT 0,
            saved_interest REAL NOT NULL DEFAULT 0,
            projected_end_date TEXT,
            notes TEXT,
            created_at TEXT NOT NULL
        );
    `);

    // 5. App Settings Table
    await client.execute(`
        CREATE TABLE IF NOT EXISTS app_settings (
            setting_key TEXT PRIMARY KEY,
            setting_value TEXT NOT NULL,
            updated_at TEXT NOT NULL
        );
    `);

    // Check seed status
    const countRes = await client.execute('SELECT COUNT(*) as count FROM installments');
    const count = Number(countRes.rows[0].count);

    if (count === 0) {
        console.log('[Database] Tabela installments vazia. Inserindo dados oficiais (60 parcelas)...');
        await seedOfficialData();
    } else {
        console.log(`[Database] Banco de dados pronto com ${count} parcelas ativas.`);
    }

    // Ensure default user and contract exist
    await ensureDefaults();
}

/**
 * Initializes database schemas and initial seed data if not present.
 * Transparently falls back to local SQLite if Turso Cloud returns 401 Unauthorized or fails.
 */
async function initDatabase() {
    const targetDesc = isUsingLocalFallback ? 'SQLite Local' : url.replace(/\/\/.*@/, '//***@');
    console.log('[Database] Inicializando banco de dados no destino:', targetDesc);

    try {
        await createTablesAndSeed();
        console.log(`✅ [Database] Banco de dados pronto (${isUsingLocalFallback ? 'SQLite Local' : 'Turso Cloud'}).`);
        return { mode: isUsingLocalFallback ? 'local' : 'turso-cloud' };
    } catch (err) {
        const errorMsg = String(err.message || err);
        const isAuthError = err.status === 401 ||
                            err.code === 'SERVER_ERROR' ||
                            errorMsg.includes('401') ||
                            String(err.cause?.status) === '401';

        if (!isUsingLocalFallback && isAuthError) {
            switchToLocalFallback('Falha de autenticação com Turso Cloud (HTTP 401 Unauthorized). Verifique o TURSO_AUTH_TOKEN.');
            await createTablesAndSeed();
            console.log('✅ [Database] Inicialização via fallback SQLite concluída com sucesso!');
            return { mode: 'local' };
        } else if (!isUsingLocalFallback) {
            switchToLocalFallback(`Falha ao conectar no Turso Cloud: ${errorMsg}`);
            await createTablesAndSeed();
            console.log('✅ [Database] Inicialização via fallback SQLite concluída com sucesso!');
            return { mode: 'local' };
        }

        throw err;
    }
}

function getDatabaseMode() {
    return isUsingLocalFallback ? 'local' : 'turso-cloud';
}

/**
 * Seeds official data from data/financing_data.json into Turso
 */
async function seedOfficialData() {
    const dataFilePath = path.join(__dirname, '..', 'data', 'financing_data.json');
    if (!fs.existsSync(dataFilePath)) {
        throw new Error('Seed file data/financing_data.json not found!');
    }

    const fileContent = JSON.parse(fs.readFileSync(dataFilePath, 'utf-8'));
    const now = new Date().toISOString();

    // Insert Default User
    await client.execute({
        sql: `INSERT OR REPLACE INTO users (id, name, email, phone, created_at, updated_at)
              VALUES ('default_user', 'Titular do Financiamento', 'contato@amortizaflow.local', '', ?, ?)`,
        args: [now, now]
    });

    // Insert Contract
    const contract = fileContent.contract;
    await client.execute({
        sql: `INSERT OR REPLACE INTO contracts (
                id, user_id, title, contract_number, total_installments, nominal_installment,
                total_contract_amount, monthly_interest_rate, start_date, original_end_date,
                current_projected_end_date, created_at, updated_at
              ) VALUES (
                'main_contract', 'default_user', ?, 'FIN-2026-60P', ?, ?,
                ?, ?, ?, ?,
                ?, ?, ?
              )`,
        args: [
            contract.title || 'Financiamento Imobiliário / Veículo',
            contract.totalInstallments || 60,
            contract.nominalInstallment || 985.38,
            contract.totalContractAmount || 59122.80,
            contract.monthlyInterestRate || 0.0165,
            contract.startDate || '14/05/2026',
            contract.originalEndDate || '14/04/2031',
            '14/01/2031',
            now,
            now
        ]
    });

    // Clear existing installments
    await client.execute("DELETE FROM installments WHERE contract_id = 'main_contract'");

    // Batch insert installments in chunks of 20
    const installments = fileContent.installments;
    const chunkSize = 20;
    for (let i = 0; i < installments.length; i += chunkSize) {
        const chunk = installments.slice(i, i + chunkSize);
        const statements = chunk.map(item => ({
            sql: `INSERT INTO installments (
                    parcel_number, contract_id, due_date, nominal_amount,
                    theoretical_amortization, interest_amount, actual_paid_amount,
                    payment_date, is_paid, is_anticipated, saved_interest,
                    notes, created_at, updated_at
                  ) VALUES (?, 'main_contract', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            args: [
                item.parcelNumber,
                item.dueDate,
                item.nominalAmount,
                item.theoreticalAmortization,
                item.interestAmount,
                item.actualPaidAmount !== undefined ? item.actualPaidAmount : null,
                item.paymentDate || null,
                item.isPaid ? 1 : 0,
                item.isAnticipated ? 1 : 0,
                item.savedInterest || 0,
                item.notes || '',
                now,
                now
            ]
        }));
        await client.batch(statements, 'write');
    }

    console.log(`[Turso] Successfully seeded ${installments.length} installments into Turso cloud database.`);
}

/**
 * Ensures default user and contract exist
 */
async function ensureDefaults() {
    const now = new Date().toISOString();
    await client.execute({
        sql: `INSERT OR IGNORE INTO users (id, name, email, phone, created_at, updated_at)
              VALUES ('default_user', 'Titular do Financiamento', 'contato@amortizaflow.local', '', ?, ?)`,
        args: [now, now]
    });

    await client.execute({
        sql: `INSERT OR IGNORE INTO contracts (
                id, user_id, title, contract_number, total_installments, nominal_installment,
                total_contract_amount, monthly_interest_rate, start_date, original_end_date,
                current_projected_end_date, created_at, updated_at
              ) VALUES (
                'main_contract', 'default_user', 'Financiamento Imobiliário / Veículo', 'FIN-2026-60P',
                60, 985.38, 59122.80, 0.0165, '14/05/2026', '14/04/2031', '14/01/2031', ?, ?
              )`,
        args: [now, now]
    });
}

/**
 * Get all installments with optional filter ('PAID', 'PENDING', 'ANTICIPATED', or undefined)
 */
async function getInstallments(statusFilter) {
    let sql = 'SELECT * FROM installments ORDER BY parcel_number ASC';
    let args = [];

    if (statusFilter === 'PAID') {
        sql = 'SELECT * FROM installments WHERE is_paid = 1 ORDER BY parcel_number ASC';
    } else if (statusFilter === 'PENDING') {
        sql = 'SELECT * FROM installments WHERE is_paid = 0 ORDER BY parcel_number ASC';
    } else if (statusFilter === 'ANTICIPATED') {
        sql = 'SELECT * FROM installments WHERE is_paid = 1 AND is_anticipated = 1 ORDER BY parcel_number ASC';
    }

    const res = await client.execute({ sql, args });
    return res.rows.map(mapRowToInstallment);
}

/**
 * Get a single installment by its number
 */
async function getInstallmentByNumber(parcelNumber) {
    const res = await client.execute({
        sql: 'SELECT * FROM installments WHERE parcel_number = ?',
        args: [parcelNumber]
    });
    if (res.rows.length === 0) return null;
    return mapRowToInstallment(res.rows[0]);
}

/**
 * Toggle payment status of an installment
 */
async function toggleInstallmentPayment(parcelNumber) {
    const item = await getInstallmentByNumber(parcelNumber);
    if (!item) return null;

    const newIsPaid = !item.isPaid;
    const now = new Date().toISOString();

    let actualPaid = null;
    let paymentDate = null;
    let savedInterest = 0;
    let isAnticipated = 0;

    if (newIsPaid) {
        // If marking as paid
        if (parcelNumber >= 50) {
            // Anticipated payment with interest savings
            actualPaid = item.theoreticalAmortization;
            savedInterest = parseFloat((item.nominalAmount - item.theoreticalAmortization).toFixed(2));
            isAnticipated = 1;
        } else {
            actualPaid = item.nominalAmount;
            savedInterest = 0;
            isAnticipated = 0;
        }
        paymentDate = new Date().toLocaleDateString('pt-BR');
    } else {
        // If unmarking payment
        actualPaid = null;
        paymentDate = null;
        savedInterest = 0;
        isAnticipated = 0;
    }

    await client.execute({
        sql: `UPDATE installments SET
                is_paid = ?,
                actual_paid_amount = ?,
                payment_date = ?,
                is_anticipated = ?,
                saved_interest = ?,
                updated_at = ?
              WHERE parcel_number = ?`,
        args: [newIsPaid ? 1 : 0, actualPaid, paymentDate, isAnticipated, savedInterest, now, parcelNumber]
    });

    return await getInstallmentByNumber(parcelNumber);
}

/**
 * Update installment details (amount, payment date, notes)
 */
async function updateInstallmentDetails(parcelNumber, { actualPaidAmount, paymentDate, notes, isPaid }) {
    const existing = await getInstallmentByNumber(parcelNumber);
    if (!existing) return null;

    const now = new Date().toISOString();
    const finalPaid = isPaid !== undefined ? (isPaid ? 1 : 0) : (existing.isPaid ? 1 : 0);
    const finalAmount = actualPaidAmount !== undefined ? (actualPaidAmount !== null ? parseFloat(actualPaidAmount) : null) : existing.actualPaidAmount;
    const finalDate = paymentDate !== undefined ? paymentDate : existing.paymentDate;
    const finalNotes = notes !== undefined ? notes : existing.notes;

    let saved = existing.savedInterest;
    if (finalPaid && finalAmount !== null) {
        saved = Math.max(0, parseFloat((existing.nominalAmount - finalAmount).toFixed(2)));
    }

    await client.execute({
        sql: `UPDATE installments SET
                is_paid = ?,
                actual_paid_amount = ?,
                payment_date = ?,
                saved_interest = ?,
                notes = ?,
                updated_at = ?
              WHERE parcel_number = ?`,
        args: [finalPaid, finalAmount, finalDate, saved, finalNotes, now, parcelNumber]
    });

    return await getInstallmentByNumber(parcelNumber);
}

/**
 * Apply extraordinary amortization (eliminating installments from the end)
 */
async function applyExtraordinaryAmortization(eliminatedNumbers, amountInvested, simType = 'REDUCE_TERM') {
    if (!Array.isArray(eliminatedNumbers) || eliminatedNumbers.length === 0) {
        return { success: false, appliedCount: 0 };
    }

    const now = new Date().toISOString();
    const today = new Date().toLocaleDateString('pt-BR');
    let totalSaved = 0;

    const statements = [];
    for (const num of eliminatedNumbers) {
        const item = await getInstallmentByNumber(num);
        if (item) {
            const paid = item.theoreticalAmortization;
            const saved = parseFloat((item.nominalAmount - paid).toFixed(2));
            totalSaved += saved;

            statements.push({
                sql: `UPDATE installments SET
                        is_paid = 1,
                        is_anticipated = 1,
                        actual_paid_amount = ?,
                        payment_date = ?,
                        saved_interest = ?,
                        notes = ?,
                        updated_at = ?
                      WHERE parcel_number = ?`,
                args: [paid, today, saved, `Amortização Extraordinária (${simType})`, now, num]
            });
        }
    }

    if (statements.length > 0) {
        await client.batch(statements, 'write');
    }

    // Log to amortization history
    await client.execute({
        sql: `INSERT INTO amortization_history (
                contract_id, simulation_type, amount_invested, installments_eliminated,
                months_shortened, saved_interest, notes, created_at
              ) VALUES ('main_contract', ?, ?, ?, ?, ?, ?, ?)`,
        args: [
            simType,
            amountInvested || 0,
            JSON.stringify(eliminatedNumbers),
            eliminatedNumbers.length,
            parseFloat(totalSaved.toFixed(2)),
            `Amortização extraordinária executada via app`,
            now
        ]
    });

    return {
        success: true,
        appliedCount: eliminatedNumbers.length,
        savedInterest: parseFloat(totalSaved.toFixed(2))
    };
}

/**
 * Calculate consolidated executive KPIs from active installments in Turso
 */
async function calculateSummary() {
    const installments = await getInstallments();
    const totalInstallments = installments.length;
    const paidList = installments.filter(i => i.isPaid);
    const remainingList = installments.filter(i => !i.isPaid);
    const anticipatedList = installments.filter(i => i.isPaid && i.isAnticipated);

    const totalContractAmount = parseFloat(installments.reduce((acc, i) => acc + i.nominalAmount, 0).toFixed(2));
    const totalPaidAmount = parseFloat(paidList.reduce((acc, i) => acc + (i.actualPaidAmount || 0), 0).toFixed(2));
    const remainingNominalBalance = parseFloat(remainingList.reduce((acc, i) => acc + i.nominalAmount, 0).toFixed(2));
    const totalSavedInterest = parseFloat(paidList.reduce((acc, i) => acc + (i.savedInterest || 0), 0).toFixed(2));
    const amortizedPrincipalPaid = parseFloat(paidList.reduce((acc, i) => acc + i.theoreticalAmortization, 0).toFixed(2));

    const nextDue = remainingList.length > 0 ? remainingList[0] : null;
    const progressPercentage = totalInstallments > 0 ? parseFloat(((paidList.length / totalInstallments) * 100).toFixed(1)) : 0;
    const monthsAdvanced = anticipatedList.length;

    return {
        totalContractAmount,
        totalPaidAmount,
        remainingNominalBalance,
        amortizedPrincipalPaid,
        totalSavedInterest,
        totalInstallments,
        paidInstallmentsCount: paidList.length,
        remainingInstallmentsCount: remainingList.length,
        anticipatedInstallmentsCount: anticipatedList.length,
        nextDueInstallment: nextDue,
        progressPercentage,
        monthsAdvanced
    };
}

/**
 * Generate monthly report showing balance evolution
 */
async function getMonthlyReports() {
    const installments = await getInstallments();
    let currentBalance = parseFloat(installments.reduce((acc, i) => acc + i.nominalAmount, 0).toFixed(2));

    return installments.map((item) => {
        const initialBal = currentBalance;
        let amortAmount = 0;
        let interestPaid = 0;
        let paymentAmount = 0;

        if (item.isPaid) {
            paymentAmount = item.actualPaidAmount || item.nominalAmount;
            amortAmount = item.theoreticalAmortization;
            interestPaid = Math.max(0, paymentAmount - amortAmount);
            currentBalance = parseFloat((currentBalance - item.nominalAmount).toFixed(2));
        }

        return {
            monthYear: item.dueDate,
            parcelNumber: item.parcelNumber,
            initialBalance: initialBal,
            paymentAmount: paymentAmount,
            amortizationAmount: amortAmount,
            interestPaid: parseFloat(interestPaid.toFixed(2)),
            savedInterest: item.savedInterest,
            finalBalance: item.isPaid ? currentBalance : initialBal,
            isPaid: item.isPaid,
            isAnticipated: item.isAnticipated,
            notes: item.notes
        };
    });
}

/**
 * Reset Turso database to the original 60 installments
 */
async function resetToOfficialData() {
    await seedOfficialData();
    return await calculateSummary();
}

module.exports = {
    get client() {
        return client;
    },
    getDatabaseMode,
    initDatabase,
    seedOfficialData,
    getInstallments,
    getInstallmentByNumber,
    toggleInstallmentPayment,
    updateInstallmentDetails,
    applyExtraordinaryAmortization,
    calculateSummary,
    getMonthlyReports,
    resetToOfficialData
};
