
"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const PaymentEngine = require("./payment-engine.js");

function createEngine() {
    return new PaymentEngine({
        ACC100: 1000,
        ACC200: 500,
        ACC300: 0
    });
}

test("1. Successful payment updates both balances", async () => {
    const engine = createEngine();

    const result = await engine.processPayment({
        senderAccountId: "ACC100",
        receiverAccountId: "ACC200",
        amount: 200,
        requestId: "REQ001"
    });

    assert.equal(result.status, "SUCCESS");
    assert.equal(engine.getBalance("ACC100"), 800);
    assert.equal(engine.getBalance("ACC200"), 700);
    assert.equal(
        engine.getBalance("ACC100") +
        engine.getBalance("ACC200") +
        engine.getBalance("ACC300"),
        1500
    );
});

test("2. Invalid amounts are rejected", async () => {
    const engine = createEngine();

    await assert.rejects(
        engine.processPayment({
            senderAccountId: "ACC100",
            receiverAccountId: "ACC200",
            amount: -50
        }),
        /positive number/
    );

    assert.equal(engine.getBalance("ACC100"), 1000);
});

test("3. Insufficient balance does not change balances", async () => {
    const engine = createEngine();

    await assert.rejects(
        engine.processPayment({
            senderAccountId: "ACC100",
            receiverAccountId: "ACC200",
            amount: 2000
        }),
        /Insufficient balance/
    );

    assert.equal(engine.getBalance("ACC100"), 1000);
    assert.equal(engine.getBalance("ACC200"), 500);
});

test("4. Duplicate requests are processed only once", async () => {
    const engine = createEngine();

    const payment = {
        senderAccountId: "ACC100",
        receiverAccountId: "ACC200",
        amount: 100,
        requestId: "DUP001"
    };

    const first = await engine.processPayment(payment);
    const second = await engine.processPayment(payment);

    assert.equal(first.status, "SUCCESS");
    assert.equal(second.duplicate, true);
    assert.equal(engine.getBalance("ACC100"), 900);
    assert.equal(engine.getBalance("ACC200"), 600);
    assert.equal(engine.getTransactions().length, 1);
});

test("5. Concurrent payments do not overspend an account", async () => {
    const engine = createEngine();

    const results = await Promise.allSettled([
        engine.processPayment({
            senderAccountId: "ACC100",
            receiverAccountId: "ACC200",
            amount: 700,
            requestId: "CON001"
        }),
        engine.processPayment({
            senderAccountId: "ACC100",
            receiverAccountId: "ACC300",
            amount: 600,
            requestId: "CON002"
        })
    ]);

    const successful = results.filter(
        result => result.status === "fulfilled"
    );

    assert.equal(successful.length, 1);
    assert.equal(engine.getBalance("ACC100"), 300);
    assert.equal(
        engine.getBalance("ACC100") +
        engine.getBalance("ACC200") +
        engine.getBalance("ACC300"),
        1500
    );
});

test("6. Simulated failure leaves balances unchanged", async () => {
    const engine = createEngine();

    await assert.rejects(
        engine.processPayment({
            senderAccountId: "ACC100",
            receiverAccountId: "ACC200",
            amount: 200,
            requestId: "FAIL001",
            simulateFailure: true
        }),
        /Simulated payment failure/
    );

    assert.equal(engine.getBalance("ACC100"), 1000);
    assert.equal(engine.getBalance("ACC200"), 500);
    assert.equal(engine.getTransactions().length, 0);
});

test("7. Same account cannot send money to itself", async () => {
    const engine = createEngine();

    await assert.rejects(
        engine.processPayment({
            senderAccountId: "ACC100",
            receiverAccountId: "ACC100",
            amount: 100
        }),
        /different/
    );
});
