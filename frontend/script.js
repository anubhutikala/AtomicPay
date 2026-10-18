const API_URL = "http://localhost:8081/api/payments";
const HISTORY_URL = `${API_URL}/history`;

// Get HTML elements
const paymentForm = document.getElementById("paymentForm");
const senderInput = document.getElementById("sender");
const receiverInput = document.getElementById("receiver");
const amountInput = document.getElementById("amount");
const transactionTable = document.getElementById("transactionTable");
const messageBox = document.getElementById("formMessage");

const totalTransactions = document.getElementById("totalTransactions");
const successfulTransactions = document.getElementById("successfulTransactions");
const failedTransactions = document.getElementById("failedTransactions");

let transactions = [];

// Display messages
function showMessage(message, isSuccess = true) {
    if (!messageBox) {
        alert(message);
        return;
    }

    messageBox.textContent = message;
    messageBox.style.color = isSuccess ? "green" : "red";
}

// Update dashboard counters using database records
function updateCounters() {
    totalTransactions.textContent = transactions.length;

    successfulTransactions.textContent = transactions.filter(
        transaction => transaction.status === "SUCCESS"
    ).length;

    failedTransactions.textContent = transactions.filter(
        transaction => transaction.status === "FAILED"
    ).length;
}

// Display transaction history
function displayTransactions() {
    transactionTable.innerHTML = "";

    if (transactions.length === 0) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");

        cell.colSpan = 5;
        cell.className = "empty-state";
        cell.textContent = "No transactions to display yet.";

        row.appendChild(cell);
        transactionTable.appendChild(row);
        updateCounters();
        return;
    }

    // Show newest transactions first
    [...transactions].reverse().forEach(transaction => {
        const row = document.createElement("tr");

        const values = [
            transaction.orderId,
            transaction.senderId,
            transaction.receiverId,
            `₹${Number(transaction.amount).toFixed(2)}`,
            transaction.status
        ];

        values.forEach(value => {
            const cell = document.createElement("td");
            cell.textContent = value ?? "-";
            row.appendChild(cell);
        });

        transactionTable.appendChild(row);
    });

    updateCounters();
}

// Load saved transactions from PostgreSQL through the API
async function loadTransactionHistory() {
    try {
        const response = await fetch(HISTORY_URL);

        if (!response.ok) {
            throw new Error("Could not load transaction history.");
        }

        transactions = await response.json();
        displayTransactions();

    } catch (error) {
        console.error("History loading error:", error);
        showMessage(
            "Unable to load transaction history. Check that the backend is running.",
            false
        );
    }
}

// Process payment
async function processPayment(event) {
    event.preventDefault();

    const senderAccountId = senderInput.value.trim();
    const receiverAccountId = receiverInput.value.trim();
    const amount = Number(amountInput.value);

    if (!senderAccountId || !receiverAccountId) {
        showMessage("Please enter both account IDs.", false);
        return;
    }

    if (senderAccountId === receiverAccountId) {
        showMessage("Sender and receiver cannot be the same.", false);
        return;
    }

    if (!Number.isFinite(amount) || amount <= 0) {
        showMessage("Enter a valid payment amount.", false);
        return;
    }

    try {
        showMessage("Processing payment...");

        const response = await fetch(API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                senderId: senderAccountId,
                receiverId: receiverAccountId,
                amount: amount
            })
        });

        const result = await response.json();

        if (!response.ok || result.success !== true) {
            throw new Error(result.message || "Payment failed.");
        }

        showMessage(
            `Payment successful! Transaction ID: ${result.transactionId}`
        );

        paymentForm.reset();

        // Reload the actual database history
        await loadTransactionHistory();

    } catch (error) {
        console.error("Payment error:", error);
        showMessage(error.message || "Unable to process payment.", false);
    }
}

// Attach form event listener
if (paymentForm) {
    paymentForm.addEventListener("submit", processPayment);
}

// Load database history when the dashboard opens
loadTransactionHistory();