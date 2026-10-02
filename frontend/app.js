const storageKeys = {
  gateway: "credito.gatewayBase",
  accessToken: "credito.accessToken",
  userId: "credito.userId",
};

const elements = {
  gatewayForm: document.querySelector("#gateway-form"),
  gatewayUrl: document.querySelector("#gateway-url"),
  result: document.querySelector("#result-output"),
  authState: document.querySelector("#auth-state"),
  solicitudId: document.querySelector("#solicitud-id"),
  mfaUserId: document.querySelector("#mfa-user-id"),
  mfaCode: document.querySelector("#mfa-code"),
  mfaQrContainer: document.querySelector("#mfa-qr-container"),
  mfaQrImage: document.querySelector("#mfa-qr-image"),
  mfaSecret: document.querySelector("#mfa-secret"),
  sendToken: document.querySelector("#send-token"),
};

const healthPaths = {
  gateway: "/internal/health/gateway",
  solicitudes: "/internal/health/solicitudes",
  evaluaciones: "/internal/health/evaluaciones",
  auth: "/internal/health/auth",
};

class HttpError extends Error {
  constructor(status, payload) {
    super(`HTTP ${status}`);
    this.status = status;
    this.payload = payload;
  }
}

function normalizeBaseUrl(value) {
  const url = new URL(value.trim());
  return url.href.replace(/\/$/, "");
}

function gatewayBase() {
  return normalizeBaseUrl(elements.gatewayUrl.value);
}

function apiUrl(path) {
  return `${gatewayBase()}${path.startsWith("/") ? path : `/${path}`}`;
}

function accessToken() {
  return sessionStorage.getItem(storageKeys.accessToken) || "";
}

function setStatus(element, state, text) {
  element.className = `status status--${state}`;
  element.textContent = text;
}

function updateAuthState() {
  const token = accessToken();
  if (token) {
    setStatus(elements.authState, "ok", "JWT disponible");
  } else {
    setStatus(elements.authState, "idle", "Sin sesión");
  }
}

function printable(payload) {
  if (typeof payload === "string") return payload;
  return JSON.stringify(payload, null, 2);
}

function writeResult(title, payload) {
  const timestamp = new Date().toLocaleTimeString("es-PE");
  elements.result.textContent = `[${timestamp}] ${title}\n\n${printable(payload)}`;
}

async function parseResponse(response) {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

async function request(path, { method = "GET", body, includeToken = true, report = true } = {}) {
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (includeToken && elements.sendToken.checked && accessToken()) {
    headers.Authorization = `Bearer ${accessToken()}`;
  }

  const startedAt = performance.now();
  const response = await fetch(apiUrl(path), {
    method,
    headers,
    credentials: "include",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const payload = await parseResponse(response);
  const elapsedMs = Math.round(performance.now() - startedAt);
  const reportPayload = { method, url: apiUrl(path), status: response.status, elapsedMs, body: payload };
  if (report) writeResult(`${method} ${path}`, reportPayload);
  if (!response.ok) throw new HttpError(response.status, reportPayload);
  return payload;
}

function persistAuth(response) {
  if (response?.mfaRequired) {
    writeResult("MFA pendiente", {
      message: "Envía nuevamente el login con mfaCode para obtener un token de acceso.",
      sessionTokenReceived: Boolean(response.sessionToken),
    });
    return;
  }
  if (response?.token) {
    sessionStorage.setItem(storageKeys.accessToken, response.token);
  }
  if (response?.user?.id) {
    sessionStorage.setItem(storageKeys.userId, response.user.id);
    elements.mfaUserId.value = response.user.id;
  }
  updateAuthState();
}

function currentSolicitudId() {
  const id = elements.solicitudId.value.trim();
  if (!id) throw new Error("Ingresa o crea primero un ID de solicitud.");
  return id;
}

async function checkHealth(name, report = true) {
  const badge = document.querySelector(`#health-${name}`);
  setStatus(badge, "loading", "Comprobando");
  try {
    const response = await fetch(healthPaths[name], { headers: { Accept: "application/json" } });
    const body = await parseResponse(response);
    if (!response.ok || body?.status !== "UP") throw new HttpError(response.status, body);
    setStatus(badge, "ok", "Disponible");
    if (report) writeResult(`Salud: ${name}`, { url: healthPaths[name], status: response.status, body });
    return { name, status: "UP", body };
  } catch (error) {
    setStatus(badge, "error", "No disponible");
    if (report) writeResult(`Salud: ${name}`, { error: error.message, detail: error.payload || "No se recibió una respuesta válida." });
    throw error;
  }
}

async function pollEvaluation() {
  const id = currentSolicitudId();
  const button = document.querySelector("#poll-evaluacion");
  button.disabled = true;
  button.textContent = "Esperando evaluación...";
  try {
    for (let attempt = 1; attempt <= 10; attempt += 1) {
      try {
        const response = await request(`/evaluaciones/${encodeURIComponent(id)}`, { report: false });
        writeResult("Evaluación disponible", { attempt, body: response });
        return;
      } catch (error) {
        if (!(error instanceof HttpError) || error.status !== 404 || attempt === 10) throw error;
        writeResult("Evaluación en proceso", { attempt, message: "Kafka y evaluacion-service aún procesan la solicitud." });
        await new Promise((resolve) => window.setTimeout(resolve, 2000));
      }
    }
  } finally {
    button.disabled = false;
    button.textContent = "Esperar evaluación automáticamente";
  }
}

elements.gatewayForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const base = gatewayBase();
    localStorage.setItem(storageKeys.gateway, base);
    await request("/actuator/health", { includeToken: false });
  } catch (error) {
    writeResult("No se pudo conectar con el gateway", { message: error.message, hint: "Verifica que Docker Compose esté iniciado y que el puerto 8080 esté disponible." });
  }
});

document.querySelector("#check-all-health").addEventListener("click", async () => {
  const results = await Promise.allSettled(Object.keys(healthPaths).map((name) => checkHealth(name, false)));
  writeResult("Comprobación de servicios", results.map((result, index) => ({
    service: Object.keys(healthPaths)[index],
    status: result.status,
    detail: result.status === "fulfilled" ? result.value.body : result.reason.message,
  })));
});

document.querySelectorAll("[data-health]").forEach((button) => {
  button.addEventListener("click", () => checkHealth(button.dataset.health));
});

document.querySelector("#solicitud-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  try {
    const response = await request("/solicitudes", {
      method: "POST",
      body: { monto: Number(form.get("monto")), plazoMeses: Number(form.get("plazo")) },
    });
    if (response?.id) elements.solicitudId.value = response.id;
  } catch (error) {
    if (!(error instanceof HttpError)) writeResult("Error al crear solicitud", { message: error.message });
  }
});

document.querySelector("#get-solicitud").addEventListener("click", async () => {
  try {
    await request(`/solicitudes/${encodeURIComponent(currentSolicitudId())}`);
  } catch (error) {
    if (!(error instanceof HttpError)) writeResult("Error al consultar solicitud", { message: error.message });
  }
});

document.querySelector("#get-evaluacion").addEventListener("click", async () => {
  try {
    await request(`/evaluaciones/${encodeURIComponent(currentSolicitudId())}`);
  } catch (error) {
    if (!(error instanceof HttpError)) writeResult("Error al consultar evaluación", { message: error.message });
  }
});

document.querySelector("#poll-evaluacion").addEventListener("click", () => {
  pollEvaluation().catch((error) => writeResult("No se obtuvo la evaluación", error.payload || { message: error.message }));
});

document.querySelector("#register-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  try {
    const response = await request("/auth/register", {
      method: "POST",
      body: { email: form.get("email"), fullName: form.get("fullName"), password: form.get("password") },
    });
    persistAuth(response);
  } catch (error) {
    if (!(error instanceof HttpError)) writeResult("Error en el registro", { message: error.message });
  }
});

document.querySelector("#login-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const body = { email: form.get("email"), password: form.get("password") };
  if (form.get("mfaCode")) body.mfaCode = form.get("mfaCode");
  try {
    const response = await request("/auth/login", { method: "POST", body });
    persistAuth(response);
  } catch (error) {
    if (!(error instanceof HttpError)) writeResult("Error en el login", { message: error.message });
  }
});

document.querySelector("#mfa-setup").addEventListener("click", async () => {
  const userId = elements.mfaUserId.value.trim() || sessionStorage.getItem(storageKeys.userId);
  if (!userId) return writeResult("MFA", { message: "Registra o inicia sesión primero para obtener el ID de usuario." });
  try {
    const response = await request(`/auth/mfa/setup/${encodeURIComponent(userId)}`, { method: "POST", body: {} });
    if (response?.qrCodeUrl) {
      elements.mfaQrImage.src = response.qrCodeUrl;
      elements.mfaSecret.textContent = response.secret || "No disponible";
      elements.mfaQrContainer.hidden = false;
    }
  } catch (error) {
    if (!(error instanceof HttpError)) writeResult("Error al preparar MFA", { message: error.message });
  }
});

document.querySelector("#mfa-verify").addEventListener("click", async () => {
  const userId = elements.mfaUserId.value.trim() || sessionStorage.getItem(storageKeys.userId);
  const code = elements.mfaCode.value.trim();
  if (!userId || !code) return writeResult("MFA", { message: "Ingresa el ID de usuario y el código TOTP de seis dígitos." });
  try {
    await request("/auth/mfa/verify", { method: "POST", body: { userId, code } });
  } catch (error) {
    if (!(error instanceof HttpError)) writeResult("Error al verificar MFA", { message: error.message });
  }
});

document.querySelector("#oauth-google").addEventListener("click", () => {
  window.location.assign(apiUrl("/auth/oauth/google"));
});
document.querySelector("#oauth-github").addEventListener("click", () => {
  window.location.assign(apiUrl("/auth/oauth/github"));
});

document.querySelector("#raw-request-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const method = document.querySelector("#raw-method").value;
  const path = document.querySelector("#raw-path").value.trim();
  const rawBody = document.querySelector("#raw-body").value.trim();
  let body;
  if (rawBody) {
    try {
      body = JSON.parse(rawBody);
    } catch {
      return writeResult("JSON inválido", { message: "El cuerpo de la consola debe ser JSON válido." });
    }
  }
  try {
    await request(path, { method, body });
  } catch (error) {
    if (!(error instanceof HttpError)) writeResult("Error en consola HTTP", { message: error.message });
  }
});

document.querySelector("#clear-result").addEventListener("click", () => {
  elements.result.textContent = "Aún no hay peticiones ejecutadas.";
});

const persistedGateway = localStorage.getItem(storageKeys.gateway);
if (persistedGateway) elements.gatewayUrl.value = persistedGateway;
const persistedUserId = sessionStorage.getItem(storageKeys.userId);
if (persistedUserId) elements.mfaUserId.value = persistedUserId;
updateAuthState();
