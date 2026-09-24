const fs = require('fs');
const path = require('path');

const NOMINAL = 985.38;

function calculateDueDate(parcelNumber) {
    const startYear = 2026;
    const startMonth = 5; // Maio
    const totalMonths = startMonth + (parcelNumber - 1);
    const year = startYear + Math.floor((totalMonths - 1) / 12);
    const month = ((totalMonths - 1) % 12) + 1;
    return `14/${String(month).padStart(2, '0')}/${year}`;
}

const list = [];

// 1. Parcelas 1 a 5
list.push({
    parcelNumber: 1,
    dueDate: "14/05/2026",
    nominalAmount: NOMINAL,
    theoreticalAmortization: 985.38,
    interestAmount: 0.00,
    actualPaidAmount: 985.38,
    paymentDate: "14/05/2026",
    isPaid: true,
    isAnticipated: false,
    savedInterest: 0.00,
    notes: "1ª Parcela - Quitação regular"
});

list.push({
    parcelNumber: 2,
    dueDate: "14/06/2026",
    nominalAmount: NOMINAL,
    theoreticalAmortization: 967.48,
    interestAmount: 17.90,
    actualPaidAmount: 978.89,
    paymentDate: "14/06/2026",
    isPaid: true,
    isAnticipated: false,
    savedInterest: 6.49,
    notes: "2ª Parcela - Amortização com economia de R$ 6,49"
});

list.push({
    parcelNumber: 3,
    dueDate: "14/07/2026",
    nominalAmount: NOMINAL,
    theoreticalAmortization: 949.91,
    interestAmount: 35.47,
    actualPaidAmount: 978.40,
    paymentDate: "14/07/2026",
    isPaid: true,
    isAnticipated: false,
    savedInterest: 6.98,
    notes: "3ª Parcela - Amortização com economia de R$ 6,98"
});

list.push({
    parcelNumber: 4,
    dueDate: "14/08/2026",
    nominalAmount: NOMINAL,
    theoreticalAmortization: 932.65,
    interestAmount: 52.73,
    actualPaidAmount: 979.89,
    paymentDate: "14/08/2026",
    isPaid: true,
    isAnticipated: false,
    savedInterest: 5.49,
    notes: "4ª Parcela - Amortização com economia de R$ 5,49"
});

list.push({
    parcelNumber: 5,
    dueDate: "14/09/2026",
    nominalAmount: NOMINAL,
    theoreticalAmortization: 915.71,
    interestAmount: 69.67,
    actualPaidAmount: 978.40,
    paymentDate: "14/09/2026",
    isPaid: true,
    isAnticipated: false,
    savedInterest: 6.98,
    notes: "5ª Parcela - Amortização com economia de R$ 6,98"
});

// 2. Parcelas 6 a 57
for (let i = 6; i <= 57; i++) {
    const ratio = (i - 1) / 59.0;
    const theoreticalAmort = parseFloat((NOMINAL - (ratio * (NOMINAL - 340.30))).toFixed(2));
    const interest = parseFloat((NOMINAL - theoreticalAmort).toFixed(2));

    list.push({
        parcelNumber: i,
        dueDate: calculateDueDate(i),
        nominalAmount: NOMINAL,
        theoreticalAmortization: theoreticalAmort,
        interestAmount: interest,
        actualPaidAmount: null,
        paymentDate: null,
        isPaid: false,
        isAnticipated: false,
        savedInterest: 0.00,
        notes: ""
    });
}

// 3. Parcelas 58 a 60
list.push({
    parcelNumber: 58,
    dueDate: "14/02/2031",
    nominalAmount: NOMINAL,
    theoreticalAmortization: 346.60,
    interestAmount: 638.78,
    actualPaidAmount: 432.85,
    paymentDate: "09/09/2026",
    isPaid: true,
    isAnticipated: true,
    savedInterest: 552.53,
    notes: "Amortização extraordinária do fim (Economia: R$ 552,53)"
});

list.push({
    parcelNumber: 59,
    dueDate: "14/03/2031",
    nominalAmount: NOMINAL,
    theoreticalAmortization: 340.30,
    interestAmount: 645.08,
    actualPaidAmount: 413.09,
    paymentDate: "09/09/2026",
    isPaid: true,
    isAnticipated: true,
    savedInterest: 572.29,
    notes: "Amortização extraordinária do fim (Economia: R$ 572,29)"
});

list.push({
    parcelNumber: 60,
    dueDate: "14/04/2031",
    nominalAmount: NOMINAL,
    theoreticalAmortization: 395.05,
    interestAmount: 590.33,
    actualPaidAmount: 395.05,
    paymentDate: "09/09/2026",
    isPaid: true,
    isAnticipated: true,
    savedInterest: 590.33,
    notes: "Última parcela quitada antecipada (Economia: R$ 590,33)"
});

const data = {
    contract: {
        title: "Financiamento Imobiliário / Veículo",
        totalInstallments: 60,
        nominalInstallment: NOMINAL,
        totalContractAmount: 59122.80,
        monthlyInterestRate: 0.0165,
        startDate: "14/05/2026",
        originalEndDate: "14/04/2031"
    },
    installments: list.sort((a, b) => a.parcelNumber - b.parcelNumber)
};

const targetPath = path.join(__dirname, '..', 'data', 'financing_data.json');
fs.writeFileSync(targetPath, JSON.stringify(data, null, 2), 'utf-8');
console.log(`Generated 60 installments in ${targetPath}`);
