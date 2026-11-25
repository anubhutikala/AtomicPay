/* AtomicPay frontend. Authentication/history endpoints are integration contracts;
   see FRONTEND_INTEGRATION.md. Never treat client-side role checks as authorization. */
const API_BASE = "http://localhost:8081/api";
const ENDPOINTS = {
  userLogin: `${API_BASE}/auth/login`,
  adminLogin: `${API_BASE}/auth/admin/login`,
  register: `${API_BASE}/auth/register`,
  userHistory: `${API_BASE}/payments/my-history`,
  adminHistory: `${API_BASE}/admin/payments`,
  payment: `${API_BASE}/payments`
};

const $ = (id) => document.getElementById(id);
const state = { role: "USER", user: null, token: null, transactions: [], filtered: [], page: "overview", registering: false };
let toastTimer;

function toast(message, error = false) {
  const el = $("toast"); el.textContent = message; el.style.background = error ? "#9e3543" : "#172447";
  el.classList.add("show"); clearTimeout(toastTimer); toastTimer = setTimeout(() => el.classList.remove("show"), 3400);
}
function setBusy(button, busy, label) { if (!button) return; button.disabled = busy; button.dataset.original = button.dataset.original || button.innerHTML; button.innerHTML = busy ? "Please wait…" : (label || button.dataset.original); }
function authHeaders() { return { "Content-Type": "application/json", ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}) }; }
async function request(url, options = {}) {
  const response = await fetch(url, { ...options, headers: { ...authHeaders(), ...(options.headers || {}) } });
  const raw = await response.text(); let data = null;
  try { data = raw ? JSON.parse(raw) : null; } catch { data = raw; }
  if (!response.ok) {
    const message = typeof data === "string" ? data : data?.message || data?.error || `Request failed (${response.status})`;
    throw new Error(message);
  }
  return data;
}
function setAuthRole(role) {
  state.role = role;
  document.querySelectorAll(".role-tab").forEach(btn => {
    const active = btn.dataset.role === role; btn.classList.toggle("active", active); btn.setAttribute("aria-selected", String(active));
  });
  $("authEyebrow").textContent = role === "ADMIN" ? "ADMINISTRATOR ACCESS" : "WELCOME BACK";
  $("authTitle").textContent = role === "ADMIN" ? "Admin sign in" : "Sign in to AtomicPay";
  $("authSubtitle").textContent = role === "ADMIN" ? "Sign in to manage and monitor all transactions." : "Access your account and transaction activity.";
  $("signupLine").classList.toggle("hidden", role === "ADMIN");
  $("forgotLink").classList.toggle("hidden", role === "ADMIN");
  $("authHint").textContent = role === "ADMIN" ? "Authorized administrators only." : "Your credentials are sent securely to the server.";
  showLoginForm(); $("authError").textContent = "";
}
function showLoginForm() { state.registering = false; $("loginForm").classList.remove("hidden"); $("registerForm").classList.add("hidden"); $("signupLine").classList.toggle("hidden", state.role === "ADMIN"); }
function showRegisterForm() { state.registering = true; $("loginForm").classList.add("hidden"); $("registerForm").classList.remove("hidden"); $("signupLine").classList.add("hidden"); }
function normalizeUser(payload) {
    const data = payload?.data || payload;

    return {
        token: data?.token || data?.accessToken,
        role: String(data?.role || state.role).toUpperCase(),
        id: data?.accountId || data?.id || "",
        userId: data?.userId || "",
        name: data?.name || data?.fullName || data?.email || "AtomicPay user",
        email: data?.email || ""
    };
}
async function handleLogin(event) {
  event.preventDefault(); const button = $("loginButton"); const error = $("authError"); error.textContent = "";
  const email = $("loginEmail").value.trim(); const password = $("loginPassword").value;
  if (!email || !password) { error.textContent = "Enter your email and password."; return; }
  setBusy(button, true);
  try {
    const url = state.role === "ADMIN" ? ENDPOINTS.adminLogin : ENDPOINTS.userLogin;
    const result = await request(url, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password }) });
    const user = normalizeUser(result);
    if (!user.token) throw new Error("The login API did not return an access token. Please ask your backend teammate to follow FRONTEND_INTEGRATION.md.");
    if (user.role !== state.role) throw new Error("This account does not have permission for the selected workspace.");
    state.user = user; state.token = user.token;
    // Session-only storage limits persistence after the browser tab/session is closed.
    sessionStorage.setItem("atomicpay_session", JSON.stringify(user));
    enterApp();
  } catch (err) { error.textContent = friendlyApiError(err, "login"); }
  finally { setBusy(button, false, 'Sign in <span>→</span>'); }
}
async function handleRegister(event) {
  event.preventDefault(); const button = $("registerButton"); const error = $("registerError"); error.textContent = "";
  const name = $("registerName").value.trim(), email = $("registerEmail").value.trim(), password = $("registerPassword").value;
  if (name.length < 2) { error.textContent = "Please enter your full name."; return; }
  if (password.length < 8) { error.textContent = "Password must contain at least 8 characters."; return; }
  setBusy(button, true);
  try { await request(ENDPOINTS.register, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ name, email, password }) }); toast("Account created. You can now sign in."); showLoginForm(); $("loginEmail").value = email; $("loginPassword").value = ""; }
  catch (err) { error.textContent = friendlyApiError(err, "register"); }
  finally { setBusy(button, false, 'Create account <span>→</span>'); }
}
function friendlyApiError(err, action) {
  if (err instanceof TypeError) return `Cannot reach the backend at ${API_BASE}. Start the Spring Boot server and check the API URL.`;
  if (/404|Not Found/i.test(err.message)) return `The ${action} endpoint is not implemented yet. Your backend teammate needs to add it; see FRONTEND_INTEGRATION.md.`;
  return err.message || "Something went wrong. Please try again.";
}
function enterApp() {
  $("authView").classList.add("hidden"); $("appView").classList.remove("hidden");
  const isAdmin = state.role === "ADMIN"; document.querySelectorAll(".user-only").forEach(el => el.classList.toggle("hidden", isAdmin)); document.querySelectorAll(".admin-only").forEach(el => el.classList.toggle("hidden", !isAdmin));
  $("profileName").textContent = state.user.name; $("profileRole").textContent = isAdmin ? "Administrator" : "User account"; $("avatarInitial").textContent = (state.user.name || "U").charAt(0).toUpperCase();
  $("welcomeTitle").textContent = `Hello, ${(state.user.name || "there").split(" ")[0]}.`;
  $("welcomeEyebrow").textContent = isAdmin ? "ADMINISTRATOR OVERVIEW" : "YOUR FINANCIAL OVERVIEW";
  $("welcomeDescription").textContent = isAdmin ? "Monitor transaction activity across AtomicPay." : "Here’s what’s happening with your transactions.";
  $("recentTitle").textContent = isAdmin ? "Latest platform transactions" : "Recent transactions";
  $("recentDescription").textContent = isAdmin ? "The most recent activity across all accounts." : "Your latest payment activity.";
  $("transactionsHeading").textContent = isAdmin ? "All transactions" : "Transactions";
  $("transactionsSubheading").textContent = isAdmin ? "Review and filter activity across all accounts." : "Search and review your payment activity.";
  $("tableScope").textContent = isAdmin ? "All transactions across every account." : "Transactions associated with your account.";
  $("quickSender").value = state.user.id || "Provided by your account"; $("sender").value = state.user.id || "Provided by your account";
  $("todayLabel").textContent = new Intl.DateTimeFormat(undefined, { weekday: "short", month: "short", day: "numeric", year: "numeric" }).format(new Date());
  navigate("overview"); loadTransactions();
}
function navigate(page) {
  if (page === "send" && state.role === "ADMIN") page = "overview";
  state.page = page; document.querySelectorAll(".page").forEach(el => el.classList.remove("active")); $(`${page}Page`).classList.add("active");
  document.querySelectorAll(".nav-item[data-page]").forEach(el => el.classList.toggle("active", el.dataset.page === page));
  $("breadcrumbPage").textContent = page === "send" ? "Send money" : page.charAt(0).toUpperCase() + page.slice(1);
  $("sidebar").classList.remove("open");
}
function parseDateTime(tx) {
  const raw = tx.createdAt || tx.timestamp || tx.transactionTime || tx.dateTime || tx.created_at;
  if (raw) { const d = new Date(raw); if (!Number.isNaN(d.getTime())) return d; }
  if (tx.date) { const d = new Date(`${tx.date}T${tx.time || "00:00:00"}`); if (!Number.isNaN(d.getTime())) return d; }
  return null;
}
function txId(tx) { return tx.orderId || tx.transactionId || tx.id || tx.txnId || "—"; }
function txStatus(tx) { return String(tx.status || "PENDING").toUpperCase(); }
function currency(value) { const n = Number(value); return Number.isFinite(n) ? new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 }).format(n) : "—"; }
function dateText(tx) { const d = parseDateTime(tx); return d ? d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }) : (tx.date || "—"); }
function timeText(tx) { const d = parseDateTime(tx); return d ? d.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" }) : (tx.time || "—"); }
function statusMarkup(status) { const s = status.toLowerCase(); const cls = s === "success" ? "status-success" : s === "failed" ? "status-failed" : "status-pending"; return `<span class="status-pill ${cls}">${escapeHtml(status)}</span>`; }
function escapeHtml(value) { return String(value ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])); }
async function loadTransactions() {
  const target = state.role === "ADMIN" ? ENDPOINTS.adminHistory : ENDPOINTS.userHistory;
  const tbody = $("recentTable"); tbody.innerHTML = '<tr><td colspan="6" class="empty-cell">Loading transaction records…</td></tr>';
  try {
    const result = await request(target); const rows = Array.isArray(result) ? result : (result?.transactions || result?.data || []);
    if (!Array.isArray(rows)) throw new Error("Unexpected transaction history response format.");
    state.transactions = rows; state.filtered = rows.slice(); renderDashboard(); renderTransactionTable();
  } catch (err) {
    state.transactions = []; state.filtered = []; renderDashboard();
    tbody.innerHTML = `<tr><td colspan="6" class="empty-cell">${escapeHtml(friendlyApiError(err, "transaction history"))}</td></tr>`;
    $("allTransactionsTable").innerHTML = `<tr><td colspan="7" class="empty-cell">${escapeHtml(friendlyApiError(err, "transaction history"))}</td></tr>`;
    toast(friendlyApiError(err, "transaction history"), true);
  }
}
function renderDashboard() {
  const rows = state.transactions, success = rows.filter(t => txStatus(t) === "SUCCESS"), failed = rows.filter(t => txStatus(t) === "FAILED");
  const volume = success.reduce((sum, t) => sum + (Number(t.amount) || 0), 0);
  $("statTotal").textContent = rows.length.toLocaleString("en-IN"); $("statSuccess").textContent = success.length.toLocaleString("en-IN"); $("statFailed").textContent = failed.length.toLocaleString("en-IN"); $("statVolume").textContent = currency(volume);
  const recent = [...rows].sort((a,b) => (parseDateTime(b)?.getTime() || 0) - (parseDateTime(a)?.getTime() || 0)).slice(0,5);
  $("recentTable").innerHTML = recent.length ? recent.map(t => `<tr><td class="id-cell">${escapeHtml(txId(t))}</td><td>${escapeHtml(t.senderId || "—")}</td><td>${escapeHtml(t.receiverId || "—")}</td><td>${escapeHtml(dateText(t))} · ${escapeHtml(timeText(t))}</td><td class="amount-cell">${currency(t.amount)}</td><td>${statusMarkup(txStatus(t))}</td></tr>`).join("") : '<tr><td colspan="6" class="empty-cell">No transaction records found.</td></tr>';
}
function applyFilters() {
  const id = $("filterId").value.trim().toLowerCase(), sender = $("filterSender").value.trim().toLowerCase(), receiver = $("filterReceiver").value.trim().toLowerCase(), from = $("filterFrom").value, to = $("filterTo").value, timeFrom = $("filterTimeFrom").value, timeTo = $("filterTimeTo").value, status = $("filterStatus").value;
  if (from && to && from > to) { toast("From date cannot be later than To date.", true); return; }
  state.filtered = state.transactions.filter(tx => {
    const txDate = parseDateTime(tx), txDateStr = txDate ? `${txDate.getFullYear()}-${String(txDate.getMonth()+1).padStart(2,"0")}-${String(txDate.getDate()).padStart(2,"0")}` : (tx.date || "").slice(0,10);
    const txTime = txDate ? `${String(txDate.getHours()).padStart(2,"0")}:${String(txDate.getMinutes()).padStart(2,"0")}` : (tx.time || "").slice(0,5);
    return (!id || txId(tx).toLowerCase().includes(id)) && (!sender || String(tx.senderId || "").toLowerCase().includes(sender)) && (!receiver || String(tx.receiverId || "").toLowerCase().includes(receiver)) && (!from || (txDateStr && txDateStr >= from)) && (!to || (txDateStr && txDateStr <= to)) && (!timeFrom || (txTime && txTime >= timeFrom)) && (!timeTo || (txTime && txTime <= timeTo)) && (!status || txStatus(tx) === status);
  });
  renderTransactionTable();
}
function renderTransactionTable() {
  const rows = state.filtered; $("resultCount").textContent = `${rows.length} matching transactions`; $("recordCount").textContent = `${rows.length} records`;
  $("allTransactionsTable").innerHTML = rows.length ? [...rows].sort((a,b)=>(parseDateTime(b)?.getTime()||0)-(parseDateTime(a)?.getTime()||0)).map(t => `<tr><td class="id-cell">${escapeHtml(txId(t))}</td><td>${escapeHtml(t.senderId || "—")}</td><td>${escapeHtml(t.receiverId || "—")}</td><td>${escapeHtml(dateText(t))}</td><td>${escapeHtml(timeText(t))}</td><td class="amount-cell">${currency(t.amount)}</td><td>${statusMarkup(txStatus(t))}</td></tr>`).join("") : '<tr><td colspan="7" class="empty-cell">No transactions match these filters.</td></tr>';
}
async function submitPayment(event, formKind) {
  event.preventDefault(); const isQuick = formKind === "quick", senderEl = $(isQuick ? "quickSender" : "sender"), receiverEl = $(isQuick ? "quickReceiver" : "receiver"), amountEl = $(isQuick ? "quickAmount" : "amount"), message = $(isQuick ? "quickPaymentMessage" : "paymentError"), button = $(isQuick ? "quickPayButton" : "paymentButton");
  message.textContent = ""; const senderId = state.user?.id, receiverId = receiverEl.value.trim(), amount = Number(amountEl.value);
  if (!senderId) { message.textContent = "Your account ID was not returned by login. Ask the backend team to include accountId in the login response."; return; }
  if (!receiverId) { message.textContent = "Enter the receiver account ID."; return; }
  if (receiverId === String(senderId)) { message.textContent = "Sender and receiver cannot be the same."; return; }
  if (!Number.isFinite(amount) || amount <= 0) { message.textContent = "Enter an amount greater than zero."; return; }
  if (amount > 100000000) { message.textContent = "Please enter a valid amount."; return; }
  setBusy(button, true);
  try {
    const result = await request(ENDPOINTS.payment, { method: "POST", body: JSON.stringify({
    orderId: crypto.randomUUID(),
    senderId,
    receiverId,
    amount
}) });
    if (result?.success === false) throw new Error(result.message || "Payment could not be completed.");
    message.style.color = "var(--green)"; message.textContent = `Payment request submitted${result?.transactionId ? ` · Transaction ${result.transactionId}` : ""}.`;
    receiverEl.value = ""; amountEl.value = ""; await loadTransactions(); toast("Payment request processed.");
  } catch (err) { message.style.color = "var(--red)"; message.textContent = friendlyApiError(err, "payment"); }
  finally { setBusy(button, false, isQuick ? 'Send payment <span>→</span>' : 'Review and send <span>→</span>'); }
}
function logout() { state.user = null; state.token = null; state.transactions = []; state.filtered = []; sessionStorage.removeItem("atomicpay_session"); $("appView").classList.add("hidden"); $("authView").classList.remove("hidden"); $("loginPassword").value = ""; setAuthRole("USER"); }
function restoreSession() {
  try { const saved = JSON.parse(sessionStorage.getItem("atomicpay_session")); if (saved?.token && saved?.role) { state.user = saved; state.token = saved.token; state.role = saved.role; enterApp(); } } catch { sessionStorage.removeItem("atomicpay_session"); }
}

document.querySelectorAll(".role-tab").forEach(btn => btn.addEventListener("click", () => setAuthRole(btn.dataset.role)));
$("loginForm").addEventListener("submit", handleLogin); $("registerForm").addEventListener("submit", handleRegister);
$("showRegister").addEventListener("click", showRegisterForm); $("showLogin").addEventListener("click", showLoginForm);
$("togglePassword").addEventListener("click", () => { const input = $("loginPassword"); input.type = input.type === "password" ? "text" : "password"; $("togglePassword").textContent = input.type === "password" ? "Show" : "Hide"; });
$("forgotLink").addEventListener("click", e => { e.preventDefault(); toast("Password reset needs a backend reset endpoint. Contact your administrator.", true); });
$("paymentForm").addEventListener("submit", e => submitPayment(e, "full")); $("quickPaymentForm").addEventListener("submit", e => submitPayment(e, "quick"));
$("logoutButton").addEventListener("click", logout); $("refreshButton").addEventListener("click", loadTransactions); $("applyFilters").addEventListener("click", applyFilters);
$("resetFilters").addEventListener("click", () => { ["filterId","filterSender","filterReceiver","filterFrom","filterTo","filterTimeFrom","filterTimeTo","filterStatus"].forEach(id => $(id).value = ""); state.filtered = state.transactions.slice(); renderTransactionTable(); });
document.querySelectorAll(".nav-item[data-page]").forEach(btn => btn.addEventListener("click", () => navigate(btn.dataset.page)));
document.querySelectorAll("[data-goto]").forEach(btn => btn.addEventListener("click", () => navigate(btn.dataset.goto)));
$("mobileMenu").addEventListener("click", () => $("sidebar").classList.toggle("open"));
setAuthRole("USER"); restoreSession();
