
/*
 * AtomicPay - Frontend behaviour
 * Standalone local simulation for frontend demonstration.
 * No real money is transferred.
 */

const paymentForm = document.getElementById("paymentForm");
const paymentButton = document.getElementById("paymentButton");
const formMessage = document.getElementById("formMessage");
const transactionTable = document.getElementById("transactionTable");

const paymentEngine = new PaymentEngine({
    ACC100: 10000,
    ACC200: 5000,
    ACC300: 2000
});

const transactions = [];

function showMessage(message, isError = false) {
    formMessage.textContent = message;
    formMessage.style.color = isError ? "#b42332" : "#217346";
}

function updateSummary() {
    document.getElementById("totalTransactions").textContent =
        transactions.length;

    document.getElementById("successfulTransactions").textContent =
        transactions.filter(t => t.status === "SUCCESS").length;

    document.getElementById("failedTransactions").textContent =
        transactions.filter(t => t.status === "FAILED").length;
}

function addCell(row, value) {
    const cell = document.createElement("td");
    cell.textContent = value ?? "—";
    row.appendChild(cell);
    return cell;
}

function displayTransactions() {
    transactionTable.replaceChildren();

    if (transactions.length === 0) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");

        cell.colSpan = 5;
        cell.className = "empty-state";
        cell.textContent = "No transactions to display yet.";

        row.appendChild(cell);
        transactionTable.appendChild(row);
        updateSummary();
        return;
    }

    transactions.slice().reverse().forEach(transaction => {
        const row = document.createElement("tr");

        addCell(row, transaction.transactionId);
        addCell(row, transaction.senderAccountId);
        addCell(row, transaction.receiverAccountId);

        addCell(row, `₹${Number(transaction.amount).toFixed(2)}`);

        const statusCell = addCell(row, "");
        const statusLabel = document.createElement("span");

        statusLabel.classList.add("status");

        if (transaction.status === "SUCCESS") {
            statusLabel.classList.add("status-success");
        } else {
            statusLabel.classList.add("status-failed");
        }

        statusLabel.textContent = transaction.status;
        statusCell.appendChild(statusLabel);

        transactionTable.appendChild(row);
    });

    updateSummary();
}

paymentForm.addEventListener("submit", async event => {
    event.preventDefault();

    const senderAccountId =
        document.getElementById("sender").value.trim();

    const receiverAccountId =
        document.getElementById("receiver").value.trim();

    const amount = Number(document.getElementById("amount").value);

    if (!senderAccountId || !receiverAccountId) {
        showMessage("Please enter both account IDs.", true);
        return;
    }

    if (senderAccountId === receiverAccountId) {
        showMessage("Sender and receiver accounts must be different.", true);
        return;
    }

    if (!Number.isFinite(amount) || amount <= 0) {
        showMessage("Enter an amount greater than zero.", true);
        return;
    }

    paymentButton.disabled = true;
    paymentButton.textContent = "Processing...";
    showMessage("");

    try {
        const result = await paymentEngine.processPayment({
            senderAccountId,
            receiverAccountId,
            amount,
            requestId: `UI-${Date.now()}-${Math.random()}`
        });

        transactions.push(result);
        displayTransactions();

        showMessage(
            `Payment successful. Transaction ID: ${result.transactionId}`
        );

        paymentForm.reset();
    } catch (error) {
        console.error("Payment simulation error:", error);

        const failedTransaction = {
            transactionId: `LOCAL-${Date.now()}`,
            senderAccountId,
            receiverAccountId,
            amount,
            status: "FAILED"
        };

        transactions.push(failedTransaction);
        displayTransactions();

        showMessage(error.message || "Payment failed.", true);
    } finally {
        paymentButton.disabled = false;
        paymentButton.textContent = "Send Payment";
    }
});

displayTransactions();
