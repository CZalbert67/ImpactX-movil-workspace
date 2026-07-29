/* Impact.X Mobile Prototype
   HTML + CSS + JS puro. Simula la app móvil: onboarding, wearable, chat interno,
   contactos/familia, rutas frecuentes, invitaciones por usuario/ID único,
   suscripción, historial y flujo completo SOS.
*/

const app = document.getElementById('app');
const toastRoot = document.getElementById('toast-root');
const KEY = 'impactx.mobile.prototype.v6.internal.chat.routes.users';
let countdownTimer = null;
let tripRefreshTimer = null;

const PLAN_RULES = {
  trial: {
    name: 'Trial', price: '$0', contactsLimit: 2, days: 24,
    sensors: ['Fuerza G', 'Micrófono', 'Frecuencia cardíaca', 'GPS'],
    maps: false, bypass: false, telemetry: true,
    description: 'Prueba de 30 días con sensores completos y límite de 2 contactos.'
  },
  basico: {
    name: 'Básico', price: '$99/mes', contactsLimit: 3, days: 30,
    sensors: ['Fuerza G', 'GPS'],
    maps: false, bypass: false, telemetry: false,
    description: 'Protección esencial con fuerza G, GPS y temporizador obligatorio de 20 segundos.'
  },
  premium: {
    name: 'Premium', price: '$199/mes', contactsLimit: 8, days: 30,
    sensors: ['Fuerza G', 'Micrófono', 'Frecuencia cardíaca', 'GPS', 'Bypass crítico'],
    maps: true, bypass: true, telemetry: true,
    description: 'Protección completa con IA, mapas, historial premium y bypass crítico.'
  }
};


const FOUR_WHEEL_VEHICLE_TYPES = ['Auto', 'SUV', 'Camioneta', 'Pickup', 'Van / Minivan'];

function vehicleTypeOptions() {
  const selectedType = FOUR_WHEEL_VEHICLE_TYPES.includes(state.vehicle.type) ? state.vehicle.type : 'Auto';
  return FOUR_WHEEL_VEHICLE_TYPES.map(type => `<option ${selectedType === type ? 'selected' : ''}>${type}</option>`).join('');
}

function sanitizeState(nextState) {
  const base = initialState();
  const safe = nextState || base;
  if (!safe.vehicle) safe.vehicle = base.vehicle;
  if (!FOUR_WHEEL_VEHICLE_TYPES.includes(safe.vehicle.type)) safe.vehicle.type = 'Auto';
  if (!safe.medical) safe.medical = base.medical;
  safe.medical.condition = safe.medical.condition || 'No tengo padecimientos relevantes';
  safe.medical.allergies = safe.medical.allergies || 'Ninguna alergia conocida';
  safe.medical.meds = safe.medical.meds || 'No tomo medicamento actualmente';
  if (!safe.user) safe.user = base.user;
  safe.user.username = safe.user.username || base.user.username;
  safe.user.appId = safe.user.appId || base.user.appId;
  safe.user.inviteCode = safe.user.inviteCode || base.user.inviteCode;
  safe.internalChatReady = safe.internalChatReady !== false;
  safe.legacySmsReady = false;
  if (!safe.messageTemplates) safe.messageTemplates = base.messageTemplates;
  if (!safe.frequentRoutes) safe.frequentRoutes = base.frequentRoutes;
  if (!safe.internalMessages) safe.internalMessages = base.internalMessages;
  if (!safe.routeDraft) safe.routeDraft = base.routeDraft;
  if (!safe.appDirectory) safe.appDirectory = base.appDirectory;
  if (!safe.chatDraft) safe.chatDraft = base.chatDraft;
  if (safe.permissions) safe.permissions.sms = false;
  if (Array.isArray(safe.contacts)) {
    safe.contacts = safe.contacts.map((c, idx) => ({ ...c, username: c.username || c.email || `contacto_${idx+1}`, appUserId: c.appUserId || `IX-CON-${String(idx+1).padStart(4,'0')}`, channel: 'Chat interno', notes: String(c.notes || '').replace(/chat interno|Chat interno|Correo de cuenta|SMS|WhatsApp|email|correo/gi, 'chat interno') }));
  }
  if (Array.isArray(safe.incidents)) {
    safe.incidents = safe.incidents.map(i => ({ ...i, channel: String(i.channel || '').replace(/API \+ Push|chat interno offline|chat interno|Chat interno|Correo de cuenta/gi, 'Chat interno') }));
  }
  return safe;
}

const initialState = () => ({
  loggedIn: false,
  onboardingComplete: false,
  activePlan: 'trial',
  trip: {
    active: false,
    paused: false,
    startedAt: null,
    startedLabel: '',
    lastEndedAt: '',
    routeName: 'Casa → Universidad Tecnológica de Tula-Tepeji',
    purpose: 'Traslado diario',
    permissionToken: '',
    gpsConsent: true,
    backgroundConsent: true,
    shareWithMonitors: true,
    autoDetectEnabled: true,
    currentSpeed: 0,
    riskLevel: 'Bajo',
    lastCheckpoint: 'Sin viaje activo'
  },
  tripHistory: [],
  internalChatReady: true,
  routeDraft: {
    selectedRouteId: 901,
    todayRouteShared: false,
    lastSharedAt: 'Sin envío interno'
  },
  chatDraft: {
    lastRecipient: 'Red familiar',
    lastCustomBody: 'Voy saliendo, cualquier cosa les aviso por aquí.'
  },
  appDirectory: [
    { username: 'maria_tejeda', appUserId: 'IX-MAR-7731', name: 'María Fernanda Tejeda', relation: 'Madre' },
    { username: 'carlos_barrera', appUserId: 'IX-CAR-7732', name: 'Carlos Barrera', relation: 'Hermano' },
    { username: 'roberto_t', appUserId: 'IX-ROB-7733', name: 'Roberto Tejeda', relation: 'Tío' },
    { username: 'ana_monroy', appUserId: 'IX-ANA-7734', name: 'Ana Monroy', relation: 'Familiar' }
  ],
  frequentRoutes: [
    { id: 901, label: 'Casa → Universidad', name: 'Ruta escuela', origin: 'Casa', destination: 'Universidad Tecnológica de Tula-Tepeji', note: 'Ruta diaria por Av. Universidad', lastUsed: 'Ayer' },
    { id: 902, label: 'Casa → Trabajo', name: 'Ruta trabajo', origin: 'Casa', destination: 'Zona centro, Tula', note: 'Ruta matutina entre semana', lastUsed: 'Hace 3 días' },
    { id: 903, label: 'Casa → Familiar', name: 'Visita familiar', origin: 'Casa', destination: 'Casa de familiar principal', note: 'Ruta frecuente de fin de semana', lastUsed: 'Hace 1 semana' }
  ],
  messageTemplates: [
    { id: 801, title: 'Inicio de ruta', body: 'Inicié mi ruta {ruta}. Podrán ver actualizaciones solo dentro de Impact.X si ocurre una alerta.' },
    { id: 802, title: 'Cambio de ruta', body: 'Actualicé mi ruta de hoy a {ruta}. Cualquier aviso se enviará por chat interno.' },
    { id: 803, title: 'Llegué bien', body: 'Llegué bien a mi destino. Cierro el seguimiento de ruta.' },
    { id: 804, title: 'Retraso', body: 'Voy con retraso en {ruta}. Mantendré el seguimiento activo desde el wearable.' },
    { id: 805, title: 'Alerta SOS', body: 'Impact.X detectó una emergencia. Abre la alerta interna para ver ubicación y estado.' }
  ],
  internalMessages: [
    { id: 1001, from: 'Sistema Impact.X', to: 'Red familiar', title: 'Bienvenida al chat interno', body: 'Las invitaciones, rutas y alertas se gestionan únicamente dentro de Impact.X.', time: '09:20', type: 'system' },
    { id: 1002, from: 'Leonardo', to: 'María Fernanda', title: 'Ruta frecuente compartida', body: 'Hoy tomaré Casa → Universidad Tecnológica de Tula-Tepeji.', time: '09:35', type: 'route' }
  ],
  appUi: { devicePreset: 'Android 412×915', density: 'Compacta' },
  appMode: 'titular',
  networkOnline: true,
  gpsReady: true,
  legacySmsReady: false,
  sqliteReady: true,
  lastSync: 'Hace 4 min',
  currentAlert: null,
  user: {
    name: 'Leonardo Isaac Barrera Tejeda',
    username: 'leo_impactx',
    appId: 'IX-LEO-2026',
    inviteCode: 'FAM-LEO-4829',
    email: 'leo.demo@impactx.mx',
    phone: '+52 773 000 0000',
    city: 'Tula de Allende, Hidalgo',
    language: 'Español'
  },
  medical: {
    blood: 'O+',
    allergies: 'Ninguna alergia conocida',
    condition: 'No tengo padecimientos relevantes',
    meds: 'No tomo medicamento actualmente',
    note: 'Contactar primero a familiar principal.'
  },
  vehicle: {
    type: 'Auto',
    brand: 'Nissan',
    model: 'Versa Sense',
    year: '2022',
    usage: 'Mixto',
    avgSpeed: '65 km/h'
  },
  permissions: {
    location: true,
    bluetooth: true,
    notifications: true,
    sms: false,
    calls: true,
    background: true,
    activity: true,
    microphone: true,
    heart: true
  },
  wearable: {
    linked: true,
    model: 'Galaxy Watch 8',
    connection: 'connected',
    battery: 82,
    version: '1.0.3-beta',
    lastSync: 'Hace 4 min',
    pairing: {
      requiredCode: '482913',
      visibleCode: '482 913',
      enteredCode: '',
      step: 'idle',
      progress: 0,
      trustToken: 'IX-LINK-ANDROID-8F2K',
      sessionId: 'SES-WEAR-20260604-001',
      phoneName: 'Android Impact.X',
      watchName: 'Galaxy Watch 8',
      lastAttempt: 'Sin intentos'
    },
    calibration: 84,
    sensors: {
      accelerometer: true,
      microphone: true,
      heartRate: true,
      gps: true,
      background: true
    }
  },
  contacts: [
    { id: 101, name: 'María Fernanda Tejeda', username: 'maria_tejeda', appUserId: 'IX-MAR-7731', relation: 'Madre', phone: '+52 773 111 2233', email: 'maria.demo@mail.com', priority: 'Principal', status: 'Activo', channel: 'Chat interno', monitorId: 301, notes: 'Contacto principal.' },
    { id: 102, name: 'Carlos Barrera', username: 'carlos_barrera', appUserId: 'IX-CAR-7732', relation: 'Hermano', phone: '+52 773 444 5566', email: 'carlos.demo@mail.com', priority: 'Secundario', status: 'Activo', channel: 'Chat interno', monitorId: null, notes: 'Recibe alertas únicamente por chat interno de Impact.X.' }
  ],
  monitors: [
    { id: 301, contactId: 101, name: 'María Fernanda Tejeda', username: 'maria_tejeda', appUserId: 'IX-MAR-7731', phone: '+52 773 111 2233', email: 'maria.demo@mail.com', status: 'Activo', token: 'IX-MON-7H2K', invitedAt: '2026-06-01 14:20', acceptedAt: '2026-06-01 14:32', expiresAt: 'Sin expiración' },
    { id: 302, contactId: null, name: 'Roberto Tejeda', username: 'roberto_t', appUserId: 'IX-ROB-7733', phone: '+52 773 777 8899', email: 'roberto.demo@mail.com', status: 'Pendiente', token: 'IX-MON-9Q4A', invitedAt: '2026-06-03 10:11', acceptedAt: 'Pendiente', expiresAt: '2026-06-10' },
    { id: 303, contactId: null, name: 'Ana Monroy', username: 'ana_monroy', appUserId: 'IX-ANA-7734', phone: '+52 773 222 3344', email: 'ana.demo@mail.com', status: 'Revocado', token: 'IX-MON-2L1Z', invitedAt: '2026-05-28 09:41', acceptedAt: '2026-05-28 09:55', expiresAt: 'Revocado' }
  ],
  incidents: [
    { id: 501, type: 'Posible caída', severity: 'Media', status: 'Cancelado por usuario', date: '2026-06-03', time: '18:42', location: 'Av. Universidad, Tula', gps: '20.0531, -99.3432', gforce: '4.8G', decibels: '89dB', heart: '104 bpm', channel: 'No enviado', response: '00:08', note: 'Falsa alarma por movimiento brusco.' },
    { id: 502, type: 'Prueba manual', severity: 'Baja', status: 'Simulación', date: '2026-06-02', time: '12:15', location: 'Casa', gps: '20.0501, -99.3401', gforce: '1.2G', decibels: '42dB', heart: '78 bpm', channel: 'Demo', response: '00:00', note: 'Prueba de botón SOS.' }
  ],
  notifications: [
    { id: 701, title: 'Wearable conectado', body: 'Galaxy Watch 8 sincronizado correctamente.', type: 'device', read: false, route: '/device' },
    { id: 702, title: 'SQLite actualizado', body: '2 contactos/familia sincronizados para chat interno.', type: 'sync', read: false, route: '/sync' },
    { id: 703, title: 'Trial activo', body: 'Quedan 24 días de prueba.', type: 'plan', read: true, route: '/subscription' }
  ]
});

let state = load();

function load() {
  try {
    const raw = localStorage.getItem(KEY);
    return sanitizeState(raw ? { ...initialState(), ...JSON.parse(raw) } : initialState());
  } catch { return sanitizeState(initialState()); }
}
function save() { localStorage.setItem(KEY, JSON.stringify(state)); }
function resetDemo() { state = initialState(); save(); toast('Demo móvil reiniciada'); go('/splash'); }
function go(path) { location.hash = path; }
function route() { return location.hash.replace('#', '') || '/splash'; }
function clearCountdown() { if (countdownTimer) clearInterval(countdownTimer); countdownTimer = null; }
function clearTripRefresh() { if (tripRefreshTimer) clearInterval(tripRefreshTimer); tripRefreshTimer = null; }
function startTripRefresh() { clearTripRefresh(); tripRefreshTimer = setInterval(() => { if (route() === '/trip-active' && state.trip.active) render(); }, 4000); }
function today() { return new Date().toISOString().slice(0, 10); }
function nowTime() { return new Date().toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' }); }
function initials(name) { return name.split(' ').map(x => x[0]).slice(0,2).join('').toUpperCase(); }
function plan() { return PLAN_RULES[state.activePlan]; }
function activeContacts() { return state.contacts.filter(c => c.status === 'Activo'); }
function unreadCount() { return state.notifications.filter(n => !n.read).length; }
function statusBadge(status) {
  const s = String(status).toLowerCase();
  if (s.includes('activo') || s.includes('enviado') || s.includes('conectado')) return 'success';
  if (s.includes('pendiente') || s.includes('trial') || s.includes('simulación')) return 'warning';
  if (s.includes('revocado') || s.includes('vencido') || s.includes('fallido') || s.includes('desconectado')) return 'danger';
  return 'info';
}
function toast(message) {
  const el = document.createElement('div');
  el.className = 'toast';
  el.textContent = message;
  toastRoot.appendChild(el);
  setTimeout(() => el.remove(), 3000);
}

function shell({ title = 'Impact.X', subtitle = 'Protección móvil', content = '', back = null, bottom = true, actions = '' }) {
  const current = route();
  const bottomNav = bottom ? `
    <nav class="bottom-nav">
      ${navItem('/home', '⌂', 'Inicio', current.startsWith('/home') || current.startsWith('/trip') || current.includes('sos') || current.includes('alert'))}
      ${navItem('/device', '◉', 'Reloj', current.startsWith('/device') || current.includes('wearable'))}
      ${navItem('/contacts', '☏', 'Contactos', current.startsWith('/contacts') || current.startsWith('/monitors') || current.startsWith('/invite-user'))}
      ${navItem('/incidents', '▤', 'Eventos', current.startsWith('/incidents') || current.startsWith('/incident') || current.startsWith('/map'))}
      ${navItem('/profile', '●', 'Perfil', current.startsWith('/profile') || current.startsWith('/settings') || current.startsWith('/subscription'))}
    </nav>` : '';
  return `
    <div class="prototype-stage">
      <div class="phone-frame">
        <div class="phone-screen">
          <div class="status-bar">
            <span>${nowTime()}</span>
            <span class="status-icons">${state.networkOnline ? '5G' : 'OFF'} · ${state.gpsReady ? 'GPS' : 'GPS?'} · ${state.wearable.battery}%</span>
          </div>
          <header class="app-topbar">
            <div class="top-left">
              ${back ? `<button class="icon-btn" onclick="go('${back}')">‹</button>` : `<div class="brand-mini">X</div>`}
              <div class="top-title"><strong>${title}</strong><span>${subtitle}</span></div>
            </div>
            <div class="row">
              ${actions}
              <button class="icon-btn" onclick="go('/notifications')">🔔${unreadCount() ? `<sup>${unreadCount()}</sup>` : ''}</button>
            </div>
          </header>
          <section class="app-content ${bottom ? '' : 'no-bottom'}">${content}</section>
          ${bottomNav}
        </div>
      </div>
    </div>`;
}
function navItem(path, icon, label, active) {
  return `<button class="nav-item ${active ? 'active' : ''}" onclick="go('${path}')"><b>${icon}</b><span>${label}</span></button>`;
}
function chip(text, type='info') { return `<span class="badge ${type}">${text}</span>`; }
function kv(label, value) { return `<div class="kpi-card"><strong>${value}</strong><span>${label}</span></div>`; }
function field(label, value, id, type='text') {
  return `<label><span class="label">${label}</span><input id="${id}" class="input" type="${type}" value="${value || ''}" /></label>`;
}

function render() {
  clearCountdown();
  clearTripRefresh();
  const r = route();
  let html;
  if (r === '/splash') html = screenSplash();
  else if (r === '/welcome') html = screenWelcome();
  else if (r === '/login') html = screenLogin();
  else if (r === '/register') html = screenRegister();
  else if (r === '/plans') html = screenPlans('/permissions');
  else if (r === '/permissions') html = screenPermissions();
  else if (r === '/driver') html = screenDriver();
  else if (r === '/medical') html = screenMedical();
  else if (r === '/vehicle') html = screenVehicle();
  else if (r === '/wearable-link') html = screenWearableLink();
  else if (r === '/wearable-code') html = screenWearableCode();
  else if (r === '/wearable-permissions') html = screenWearablePermissions();
  else if (r === '/calibration') html = screenCalibration();
  else if (r === '/contacts-setup') html = screenContactsSetup();
  else if (r === '/monitoring-setup') html = screenMonitoringSetup();
  else if (r === '/activation-summary') html = screenActivationSummary();
  else if (r === '/home') html = screenHome();
  else if (r === '/trip-start') html = screenTripStart();
  else if (r === '/trip-active') html = screenTripActive();
  else if (r === '/trip-summary') html = screenTripSummary();
  else if (r === '/routes') html = screenFrequentRoutes();
  else if (r === '/chat') html = screenInternalChat();
  else if (r === '/templates') html = screenMessageTemplates();
  else if (r === '/invite-user') html = screenInviteUser();
  else if (r === '/device') html = screenDevice();
  else if (r === '/contacts') html = screenContacts();
  else if (r === '/contact-form') html = screenContactForm();
  else if (r.startsWith('/contact/')) html = screenContactDetail(Number(r.split('/')[2]));
  else if (r === '/plan-limit') html = screenPlanLimit();
  else if (r === '/monitors') html = screenMonitors();
  else if (r === '/monitor-form') html = screenMonitorForm();
  else if (r.startsWith('/monitor/')) html = screenMonitorDetail(Number(r.split('/')[2]));
  else if (r === '/subscription') html = screenSubscription();
  else if (r === '/change-plan') html = screenPlans('/subscription');
  else if (r === '/payment') html = screenPayment();
  else if (r === '/subscription-expired') html = screenExpired();
  else if (r === '/incidents') html = screenIncidents();
  else if (r.startsWith('/incident/')) html = screenIncidentDetail(Number(r.split('/')[2]));
  else if (r.startsWith('/map/')) html = screenMap(Number(r.split('/')[2]));
  else if (r === '/notifications') html = screenNotifications();
  else if (r === '/profile') html = screenProfile();
  else if (r === '/settings') html = screenSettings();
  else if (r === '/security') html = screenSecurity();
  else if (r === '/privacy') html = screenPrivacy();
  else if (r === '/sync') html = screenSync();
  else if (r === '/help') html = screenHelp();
  else if (r === '/sos-confirm') html = screenSosConfirm();
  else if (r === '/sos-countdown') html = screenSosCountdown();
  else if (r === '/false-alarm') html = screenFalseAlarm();
  else if (r === '/sending-alert') html = screenSendingAlert();
  else if (r === '/offline-sms') html = screenOfflineSms();
  else if (r === '/active-alert') html = screenActiveAlert();
  else if (r === '/close-incident') html = screenCloseIncident();
  else if (r === '/invite-accept') html = screenInviteAccept();
  else if (r === '/monitor-home') html = screenMonitorHome();
  else if (r === '/monitor-alert') html = screenMonitorAlert();
  else html = screen404();
  app.innerHTML = html;
  if (r === '/sos-countdown') startCountdown();
  if (r === '/trip-active') startTripRefresh();
}

function screenSplash() {
  return shell({ title: 'Impact.X', subtitle: 'Validando sesión móvil', bottom: false, actions: '', content: `
    <div class="hero-mobile">
      <div class="logo-xl">X</div>
      <div>
        <h1 class="h1">Impact.X Móvil</h1>
        <p class="lead">Puente operativo entre smartwatch, GPS, chat interno, rutas frecuentes, contactos/familia y backend.</p>
      </div>
      <div class="card soft">
        <div class="row between"><span>Estado de sesión</span>${chip(state.loggedIn ? 'Sesión encontrada' : 'Sin sesión', state.loggedIn ? 'success' : 'warning')}</div>
        <div class="row between"><span>SQLite local</span>${chip(state.sqliteReady ? 'Disponible' : 'Pendiente', state.sqliteReady ? 'success' : 'warning')}</div>
        <div class="row between"><span>Wearable</span>${chip(state.wearable.linked ? 'Vinculado' : 'No vinculado', state.wearable.linked ? 'success' : 'danger')}</div>
      </div>
      <button class="btn primary block" onclick="go('${state.loggedIn ? '/home' : '/welcome'}')">Continuar</button>
      <button class="btn ghost block" onclick="resetDemo()">Reiniciar simulación</button>
    </div>` });
}
function screenWelcome() {
  return shell({ title: 'Bienvenida', subtitle: 'Seguridad privada de emergencia', bottom: false, back: '/splash', actions: '', content: `
    <div class="hero-mobile">
      <div class="logo-xl">X</div>
      <div>
        <h1 class="h1">Tu burbuja de seguridad privada.</h1>
        <p class="lead">Detecta posibles choques desde el smartwatch; el móvil muestra estado, rutas y comunicación interna con tu red familiar.</p>
      </div>
      <div class="grid-2">
        ${kv('Red interna', activeContacts().length)}
        ${kv('Plan demo', plan().name)}
      </div>
      <button class="btn primary block" onclick="go('/login')">Iniciar sesión</button>
      <button class="btn block" onclick="go('/register')">Crear cuenta</button>
      <button class="btn ghost block" onclick="go('/plans')">Ver planes antes de registrarme</button>
      <button class="btn ghost block" onclick="go('/invite-accept')">Soy monitor invitado</button>
    </div>` });
}
function screenLogin() {
  return shell({ title: 'Iniciar sesión', subtitle: 'Correo o usuario Impact.X', bottom: false, back: '/welcome', actions: '', content: `
    <div class="section-title"><h2>Acceso Impact.X</h2><p>Ingresa con tu correo o con tu nombre de usuario único. Al iniciar sesión no se vuelve a elegir plan; la app carga el plan que ya existe en tu cuenta.</p></div>
    <div class="card identity-card">
      <div class="row"><div class="avatar">${initials(state.user.name)}</div><div><strong>${state.user.username}</strong><p>ID único: ${state.user.appId}</p></div></div>
    </div>
    <div class="card">
      <div class="form-grid">
        <label><span class="label">Correo o nombre de usuario</span><input id="loginIdentity" class="input" value="${state.user.username}" placeholder="leo_impactx o correo@dominio.com" /></label>
        <label><span class="label">Contraseña</span><input class="input" type="password" value="impactx123" /></label>
        <button class="btn primary block" onclick="loginDemo()">Entrar con mi cuenta</button>
        <button class="btn ghost block" onclick="toast('Se envió una recuperación interna simulada')">Olvidé mi contraseña</button>
      </div>
    </div>
    <button class="btn block" onclick="go('/register')">Crear cuenta nueva</button>` });
}
function screenRegister() {
  return shell({ title: 'Registro', subtitle: 'Cuenta del conductor titular', bottom: false, back: '/welcome', actions: '', content: `
    <div class="section-title"><h2>Crear titular</h2><p>El conductor administra sus contactos, rutas, wearable y red familiar. El nombre de usuario será único e irrepetible dentro de Impact.X.</p></div>
    <div class="card"><div class="form-grid">
      ${field('Nombre completo', state.user.name, 'regName')}
      ${field('Nombre de usuario único', state.user.username, 'regUsername')}
      ${field('Correo de cuenta', state.user.email, 'regEmail', 'email')}
      ${field('Teléfono', state.user.phone, 'regPhone')}
      ${field('Código de invitación opcional', '', 'regInviteCode')}
      <label><span class="label">Contraseña</span><input class="input" type="password" value="impactx123" /></label>
      <div class="card soft"><strong>ID que se generará</strong><p>Ejemplo: IX-${state.user.username.toUpperCase().replace(/[^A-Z0-9]/g,'').slice(0,3) || 'USR'}-${new Date().getFullYear()}</p></div>
      <label class="row"><input type="checkbox" checked /> <span class="subtle">Acepto términos y aviso de privacidad.</span></label>
      <button class="btn primary block" onclick="registerDemo()">Crear cuenta y elegir plan inicial</button>
    </div></div>` });
}
function screenPlans(next = '/permissions') {
  const inApp = state.loggedIn && state.onboardingComplete;
  return shell({ title: 'Planes', subtitle: 'Trial, Básico y Premium', bottom: inApp, back: inApp ? '/subscription' : '/welcome', actions: '', content: `
    <div class="section-title"><h2>Selecciona un plan</h2><p>La app móvil aplica límites de contactos y sensores según el plan.</p></div>
    ${Object.entries(PLAN_RULES).map(([key, p]) => `
      <div class="card ${state.activePlan === key ? 'success' : ''}">
        <div class="row between"><h3>${p.name}</h3>${chip(p.price, key === 'premium' ? 'primary' : 'info')}</div>
        <p>${p.description}</p>
        <div class="row wrap" style="margin:12px 0">${p.sensors.map(s => chip(s, 'info')).join('')}</div>
        <div class="grid-2">
          ${kv('Límite contactos', p.contactsLimit)}
          ${kv('Bypass crítico', p.bypass ? 'Sí' : 'No')}
        </div>
        <button class="btn ${key === 'premium' ? 'primary' : ''} block" onclick="selectPlan('${key}', '${next}')">${state.activePlan === key ? 'Plan actual' : 'Elegir plan'}</button>
      </div>`).join('')}` });
}
function screenPermissions() {
  const items = [
    ['location', 'Ubicación GPS', 'Necesaria para enviar coordenadas exactas.'],
    ['bluetooth', 'Bluetooth', 'Comunicación con Galaxy Watch/Wear OS.'],
    ['notifications', 'Notificaciones', 'Avisos de riesgo, batería y alertas.'],
    ['sms', 'Chat interno', 'Mensajería privada dentro de Impact.X.'],
    ['calls', 'Llamada directa', 'Acción rápida opcional; no es canal de notificación automática.'],
    ['background', 'Segundo plano', 'Mantener puente móvil activo.'],
    ['activity', 'Actividad física', 'Validación de movimiento.'],
    ['microphone', 'Micrófono', 'Solo para Trial/Premium con detección acústica.'],
    ['heart', 'Frecuencia cardíaca', 'Lectura PPG desde smartwatch.']
  ];
  return shell({ title: 'Permisos', subtitle: 'Protección móvil real', bottom: false, back: '/plans', actions: '', content: `
    <div class="section-title"><h2>Permisos críticos</h2><p>En una app real se pedirían al sistema operativo. Aquí puedes simularlos.</p></div>
    ${items.map(([key, name, desc]) => `
      <div class="list-item row between">
        <div><strong>${name}</strong><p>${desc}</p></div>
        <button class="btn small ${state.permissions[key] ? 'success' : 'warning'}" onclick="togglePermission('${key}')">${state.permissions[key] ? 'Activo' : 'Activar'}</button>
      </div>`).join('')}
    <button class="btn primary block" onclick="go('/driver')">Continuar</button>
    <button class="btn ghost block" onclick="go('/help')">Ayuda de permisos</button>` });
}
function screenDriver() {
  return shell({ title: 'Perfil conductor', subtitle: 'Datos del titular', bottom: false, back: '/permissions', actions: '', content: `
    <div class="section-title"><h2>Datos principales</h2><p>Estos datos se sincronizan con la web y contextualizan las alertas.</p></div>
    <div class="card"><div class="form-grid">
      ${field('Nombre completo', state.user.name, 'userName')}
      ${field('Nombre de usuario único', state.user.username, 'userUsername')}
      <label><span class="label">ID Impact.X</span><input class="input" value="${state.user.appId}" disabled /></label>
      ${field('Teléfono', state.user.phone, 'userPhone')}
      ${field('Ciudad', state.user.city, 'userCity')}
      <button class="btn primary block" onclick="saveUserProfile('/medical')">Guardar y continuar</button>
    </div></div>` });
}
function screenMedical() {
  return shell({ title: 'Datos médicos', subtitle: 'Información para emergencia', bottom: false, back: '/driver', actions: '', content: `
    <div class="section-title"><h2>Ficha médica de emergencia</h2><p>Contesta estas preguntas de forma concreta. Los monitores autorizados solo podrán ver esta información durante una alerta SOS.</p></div>
    <div class="card soft">
      <div class="row between"><div><strong>Uso en emergencia</strong><p>Estos datos se adjuntan al reporte móvil para que tus contactos sepan qué decir o revisar primero.</p></div>${chip('Privado','info')}</div>
    </div>
    <div class="card"><div class="form-grid">
      ${field('Tipo de sangre', state.medical.blood, 'blood')}
      ${field('¿Tienes algún padecimiento o condición médica?', state.medical.condition, 'condition')}
      ${field('¿Qué alergias tienes?', state.medical.allergies, 'allergies')}
      ${field('¿Qué medicamento tomas actualmente?', state.medical.meds, 'meds')}
      <label><span class="label">Nota de emergencia para tus contactos</span><textarea id="medNote" class="textarea">${state.medical.note}</textarea></label>
      <button class="btn primary block" onclick="saveMedical('/vehicle')">Guardar y continuar</button>
      <button class="btn ghost block" onclick="go('/vehicle')">Omitir por ahora</button>
    </div></div>` });
}
function screenVehicle() {
  return shell({ title: 'Vehículo', subtitle: 'Contexto para IA Edge', bottom: false, back: '/medical', actions: '', content: `
    <div class="section-title"><h2>Datos de conducción</h2><p>Ayudan a distinguir un bache fuerte de una colisión real. Esta versión está enfocada únicamente en vehículos de 4 ruedas.</p></div>
    <div class="card soft"><div class="row between"><div><strong>Vehículos permitidos</strong><p>Autos, SUV, camionetas, pickups y vans para mantener el caso de uso solo en vehículos de 4 ruedas.</p></div>${chip('4 ruedas','info')}</div></div>
    <div class="card"><div class="form-grid">
      <label><span class="label">Tipo de vehículo de 4 ruedas</span><select id="vehicleType" class="select">${vehicleTypeOptions()}</select></label>
      ${field('Marca', state.vehicle.brand, 'vehicleBrand')}
      ${field('Modelo', state.vehicle.model, 'vehicleModel')}
      ${field('Año', state.vehicle.year, 'vehicleYear')}
      ${field('Velocidad promedio', state.vehicle.avgSpeed, 'avgSpeed')}
      <button class="btn primary block" onclick="saveVehicle('/wearable-link')">Guardar y vincular reloj</button>
    </div></div>` });
}
function ensurePairing() {
  if (!state.wearable.pairing) {
    state.wearable.pairing = {
      requiredCode: '482913',
      visibleCode: '482 913',
      enteredCode: '',
      step: 'idle',
      progress: 0,
      trustToken: 'IX-LINK-ANDROID-8F2K',
      sessionId: 'SES-WEAR-20260604-001',
      phoneName: 'Android Impact.X',
      watchName: state.wearable.model || 'Galaxy Watch 8',
      lastAttempt: 'Sin intentos'
    };
  }
  return state.wearable.pairing;
}
function pairingStepText(step) {
  const labels = {
    idle: 'Esperando código',
    validating: 'Validando código',
    handshake: 'Creando canal seguro',
    permissions: 'Solicitando permisos',
    sync: 'Sincronizando perfil',
    linked: 'Vinculado correctamente',
    error: 'Código incorrecto'
  };
  return labels[step] || 'Esperando código';
}
function screenWearableLink() {
  const pair = ensurePairing();
  const isLinked = state.wearable.linked && state.wearable.connection === 'connected';
  return shell({ title: 'Vincular reloj', subtitle: 'Wear OS / Galaxy Watch', bottom: false, back: '/vehicle', actions: '', content: `
    <div class="section-title"><h2>Dispositivo cercano</h2><p>Simulación de búsqueda Bluetooth Low Energy entre Android y Wear OS.</p></div>
    <div class="card pairing-hero">
      <div class="row between"><div><h3>${state.wearable.model}</h3><p>${isLinked ? 'Vinculado y autorizado para Impact.X' : 'Detectado cerca del teléfono'}</p></div>${chip(isLinked ? 'Vinculado' : 'Encontrado', isLinked ? 'success' : 'info')}</div>
      <div class="watch-preview" aria-label="Reloj Wear OS simulado"><span>${pair.visibleCode}</span><small>Código mostrado en el reloj</small></div>
      <div class="grid-2" style="margin-top:12px">${kv('Batería', state.wearable.battery + '%')}${kv('Versión', state.wearable.version)}${kv('Sesión', pair.sessionId.slice(-7))}${kv('Estado', pairingStepText(pair.step))}</div>
      <button class="btn primary block" onclick="go('/wearable-code')">${isLinked ? 'Revisar sincronización por código' : 'Sincronizar con código'}</button>
      <button class="btn block" onclick="refreshPairCode()">Generar nuevo código</button>
      <button class="btn ghost block" onclick="go('/contacts-setup')">Omitir y usar protección limitada</button>
    </div>
    <div class="card soft"><h3>¿Por qué código?</h3><p>En una app final, el código evita vincular el reloj equivocado. El teléfono valida el PIN temporal, crea un token seguro, activa permisos y sincroniza el perfil del conductor.</p></div>` });
}
function screenWearableCode() {
  const pair = ensurePairing();
  const clean = String(pair.enteredCode || '').replace(/\D/g, '').slice(0, 6);
  const pct = Math.max(0, Math.min(100, pair.progress || 0));
  const isLinked = state.wearable.linked && state.wearable.connection === 'connected';
  return shell({ title: 'Código de sincronización', subtitle: 'Teléfono ↔ Wearable', bottom: false, back: '/wearable-link', actions: '', content: `
    <div class="section-title"><h2>Ingresa el código del reloj</h2><p>Simula la pantalla exacta donde el usuario confirma que el teléfono Android y el Galaxy Watch pertenecen a la misma sesión.</p></div>
    <div class="card pairing-card ${pair.step === 'error' ? 'danger' : isLinked ? 'success' : ''}">
      <div class="row between"><div><h3>${pair.watchName}</h3><p>${pair.phoneName} · ${pair.sessionId}</p></div>${chip(pairingStepText(pair.step), pair.step === 'error' ? 'danger' : isLinked ? 'success' : 'warning')}</div>
      <div class="pair-code-source">
        <span>${pair.visibleCode}</span>
        <small>Este es el código que aparecería en la app del smartwatch.</small>
      </div>
      <label><span class="label">Código de 6 dígitos</span><input id="wearCode" class="input code-input" inputmode="numeric" maxlength="7" placeholder="000 000" value="${formatPairCode(clean)}" oninput="handlePairInput(this)" /></label>
      <div class="numpad">
        ${[1,2,3,4,5,6,7,8,9].map(n => `<button class="num-btn" onclick="tapPairDigit('${n}')">${n}</button>`).join('')}
        <button class="num-btn muted" onclick="clearPairCode()">⌫</button>
        <button class="num-btn" onclick="tapPairDigit('0')">0</button>
        <button class="num-btn ok" onclick="validatePairCode()">OK</button>
      </div>
      <div class="progress"><span style="width:${pct}%"></span></div>
      <div class="timeline pairing-timeline">
        <div class="timeline-item ${pct >= 20 ? 'done' : ''}"><div class="dot"></div><div><strong>1. Código temporal</strong><p>El reloj muestra un PIN válido solo para esta sesión.</p></div></div>
        <div class="timeline-item ${pct >= 45 ? 'done' : ''}"><div class="dot"></div><div><strong>2. Handshake seguro</strong><p>El móvil crea un token local para reconocer este wearable.</p></div></div>
        <div class="timeline-item ${pct >= 70 ? 'done' : ''}"><div class="dot"></div><div><strong>3. Permisos Wear OS</strong><p>Se preparan acelerómetro, GPS, micrófono, PPG y segundo plano.</p></div></div>
        <div class="timeline-item ${pct >= 100 ? 'done' : ''}"><div class="dot"></div><div><strong>4. Sincronización final</strong><p>Perfil, plan, contactos y reglas SOS quedan asociados.</p></div></div>
      </div>
      <div class="grid-2" style="margin-top:12px">${kv('Token', pair.trustToken)}${kv('Último intento', pair.lastAttempt)}${kv('Canal', state.networkOnline ? 'Bluetooth + Nube' : 'Bluetooth local')}${kv('SQLite', state.sqliteReady ? 'Listo' : 'Pendiente')}</div>
      ${isLinked ? `<button class="btn success block" onclick="continueAfterPairing()">Continuar a permisos del reloj</button>` : `<button class="btn primary block" onclick="validatePairCode()">Validar y vincular</button>`}
      <button class="btn block" onclick="autoFillPairCode()">Autocompletar demo</button>
      <button class="btn ghost block" onclick="refreshPairCode()">Generar otro código</button>
    </div>
    <div class="card warning"><h3>Simulación realista</h3><p>Si ingresas un código incorrecto, la app no vincula el reloj. Usa el código mostrado arriba o toca autocompletar demo.</p></div>` });
}

function screenWearablePermissions() {
  const sensors = [['accelerometer','Acelerómetro'],['microphone','Micrófono'],['heartRate','Frecuencia cardíaca'],['gps','GPS'],['background','Segundo plano']];
  return shell({ title: 'Permisos reloj', subtitle: 'Lectura de sensores', bottom: false, back: '/wearable-code', actions: '', content: `
    <div class="section-title"><h2>Sensores Wear OS</h2><p>Estos sensores disparan la alerta inicial desde el smartwatch.</p></div>
    ${sensors.map(([key, label]) => `<div class="list-item row between"><strong>${label}</strong><button class="btn small ${state.wearable.sensors[key]?'success':'warning'}" onclick="toggleWearableSensor('${key}')">${state.wearable.sensors[key]?'Activo':'Activar'}</button></div>`).join('')}
    <button class="btn primary block" onclick="go('/calibration')">Calibrar sensores</button>` });
}
function screenCalibration() {
  return shell({ title: 'Calibración', subtitle: 'IA en dispositivo', bottom: false, back: '/wearable-permissions', actions: '', content: `
    <div class="section-title"><h2>Línea base del usuario</h2><p>La app simula aprendizaje de aceleración, ruido y ritmo cardíaco.</p></div>
    <div class="card">
      <div class="row between"><h3>Progreso</h3>${chip(state.wearable.calibration + '%', 'primary')}</div>
      <div class="progress"><span style="width:${state.wearable.calibration}%"></span></div>
      <div class="sensor-grid" style="margin-top:14px">
        <div class="sensor"><b>Fuerza G</b><strong>Normal</strong><span>0.9G - 1.4G</span></div>
        <div class="sensor"><b>Ruido</b><strong>42dB</strong><span>Ambiente base</span></div>
        <div class="sensor"><b>Ritmo</b><strong>78 bpm</strong><span>Reposo</span></div>
        <div class="sensor"><b>Movimiento</b><strong>Estable</strong><span>Patrón inicial</span></div>
      </div>
      <button class="btn primary block" onclick="completeCalibration()">Completar calibración</button>
    </div>` });
}
function screenContactsSetup() {
  return shell({ title: 'Contactos iniciales', subtitle: `${activeContacts().length}/${plan().contactsLimit} usados`, bottom: false, back: '/calibration', actions: '', content: `
    <div class="section-title"><h2>Contactos de emergencia</h2><p>Se sincronizan con la nube y el chat interno para alertas privadas dentro de Impact.X.</p></div>
    ${contactList(false)}
    <button class="btn block" onclick="go('/contact-form')">Agregar contacto</button>
    <button class="btn primary block" onclick="go('/monitoring-setup')">Continuar a red de monitoreo</button>` });
}
function screenMonitoringSetup() {
  return shell({ title: 'Red de monitoreo', subtitle: 'Solicitudes por usuario/ID', bottom: false, back: '/contacts-setup', actions: '', content: `
    <div class="section-title"><h2>Monitores</h2><p>Familiares que aceptan invitación y reciben geolocalización durante emergencias.</p></div>
    ${monitorList(false)}
    <button class="btn block" onclick="go('/invite-user')">Invitar por usuario/ID</button>
    <button class="btn primary block" onclick="go('/activation-summary')">Ver resumen de activación</button>` });
}
function screenActivationSummary() {
  const complete = activeContacts().length > 0 && state.wearable.linked && state.sqliteReady;
  return shell({ title: 'Resumen', subtitle: complete ? 'Protección lista' : 'Protección limitada', bottom: false, back: '/monitoring-setup', actions: '', content: `
    <div class="section-title"><h2>Activación Impact.X</h2><p>Validación final antes de entrar al dashboard móvil.</p></div>
    <div class="card ${complete ? 'success' : 'warning'}">
      <div class="row between"><strong>Estado general</strong>${chip(complete ? 'Protegido' : 'Limitado', complete ? 'success' : 'warning')}</div>
      <div class="grid-2" style="margin-top:12px">${kv('Plan', plan().name)}${kv('Contactos', `${activeContacts().length}/${plan().contactsLimit}`)}${kv('Wearable', state.wearable.linked ? 'OK' : 'No')}${kv('SQLite', state.sqliteReady ? 'OK' : 'No')}</div>
    </div>
    <button class="btn primary block" onclick="finishOnboarding()">Entrar a la app</button>` });
}

function screenHome() {
  const protectedOk = state.wearable.linked && activeContacts().length > 0 && state.sqliteReady;
  return shell({ title: 'Protección activa', subtitle: protectedOk ? 'Sistema operativo' : 'Revisión requerida', actions: `<button class="icon-btn" onclick="go('/sync')">↻</button>`, content: `
    <div class="card ${protectedOk ? 'success' : 'warning'}">
      <div class="row between"><div><h2>${protectedOk ? 'Estás protegido' : 'Protección limitada'}</h2><p>${protectedOk ? 'El móvil está enlazado con wearable, contactos, nube y SQLite.' : 'Revisa wearable, contactos o permisos para completar la protección.'}</p></div>${chip(protectedOk ? 'Activo' : 'Limitado', protectedOk ? 'success' : 'warning')}</div>
    </div>
    <div class="grid-2">
      ${kv('Plan', plan().name)}
      ${kv('Contactos', `${activeContacts().length}/${plan().contactsLimit}`)}
      ${kv('Wearable', state.wearable.connection === 'connected' ? 'Online' : 'Offline')}
      ${kv('Última sync', state.lastSync)}
    </div>
    ${tripHomeCard()}
    <div class="card action-hub">
      <div class="row between"><div><strong>Acciones rápidas</strong><p>Rutas, chat e invitaciones internas sin depender de WhatsApp, SMS ni correo.</p></div>${chip('App interna','success')}</div>
      <div class="quick-grid" style="margin-top:12px">
        <button class="quick-action" onclick="go('/routes')"><b>⌖</b><span>Preparar ruta de hoy</span></button>
        <button class="quick-action" onclick="go('/chat')"><b>💬</b><span>Abrir chat interno</span></button>
        <button class="quick-action" onclick="go('/invite-user')"><b>＋</b><span>Invitar por usuario/ID</span></button>
      </div>
    </div>
    <div class="section-title"><h2>Estado en vivo</h2><p>Datos simulados recibidos del smartwatch.</p></div>
    <div class="sensor-grid">
      <div class="sensor"><b>Fuerza G</b><strong>1.1G</strong><span>Normal</span></div>
      <div class="sensor"><b>Ruido</b><strong>${plan().telemetry ? '44dB' : 'Bloqueado'}</strong><span>${plan().telemetry ? 'Ambiente' : 'Solo Premium/Trial'}</span></div>
      <div class="sensor"><b>Ritmo</b><strong>${plan().telemetry ? '82 bpm' : 'Bloqueado'}</strong><span>${plan().telemetry ? 'Estable' : 'Solo Premium/Trial'}</span></div>
      <div class="sensor"><b>GPS</b><strong>${state.gpsReady ? 'Listo' : 'Pendiente'}</strong><span>Tula, Hidalgo</span></div>
    </div>
    <div class="card soft">
      <div class="row between"><strong>Simulación rápida</strong>${chip(state.networkOnline ? 'Internet activo' : 'Modo offline', state.networkOnline ? 'success' : 'warning')}</div>
      <div class="row wrap" style="margin-top:12px">
        <button class="btn small" onclick="simulateBump()">Bache / falsa alarma</button>
        <button class="btn small warning" onclick="go('/sos-countdown')">Posible choque</button>
        <button class="btn small danger" onclick="criticalImpact()">Impacto crítico</button>
        <button class="btn small" onclick="toggleNetwork()">${state.networkOnline ? 'Quitar internet' : 'Restaurar internet'}</button>
      </div>
    </div>
    <div class="floating-sos"><button class="sos-main" onclick="go('/sos-confirm')">SOS MANUAL</button></div>
  ` });
}
function tripHomeCard() {
  const t = state.trip || initialState().trip;
  if (t.active) {
    const live = tripTelemetry();
    return `
      <div class="card primary-trip">
        <div class="row between"><div><h2>Viaje iniciado desde wearable</h2><p>${t.routeName}</p></div>${chip(t.paused ? 'Pausado' : 'Conduciendo', t.paused ? 'warning' : 'success')}</div>
        <div class="grid-3" style="margin-top:12px">
          ${kv('Tiempo', tripElapsedLabel())}
          ${kv('Velocidad', live.speed + ' km/h')}
          ${kv('Riesgo', live.risk)}
        </div>
        <button class="btn primary block" onclick="go('/trip-active')">Ver viaje activo</button>
      </div>`;
  }
  const selected = selectedRoute();
  return `
    <div class="card start-trip-card">
      <div class="row between"><div><h2>Modo conducción</h2><p>El viaje ya no se inicia desde el móvil. Se activa desde el wearable y aquí solo se visualiza el estado.</p></div>${chip('Esperando reloj', 'warning')}</div>
      <div class="route-mini"><strong>Ruta de hoy</strong><span>${selected ? selected.label : 'Sin ruta seleccionada'}</span></div>
      <div class="row wrap" style="margin-top:12px">
        <button class="btn small" onclick="go('/routes')">Rutas frecuentes</button>
        <button class="btn small" onclick="go('/chat')">Chat interno</button>
        <button class="btn small" onclick="go('/device')">Ver wearable</button>
      </div>
    </div>`;
}

function tripChecks() {
  const checks = [
    { key:'session', label:'Sesión del titular activa', ok: state.loggedIn, fix:'/login' },
    { key:'plan', label:`Plan ${plan().name} vigente`, ok: true, fix:'/subscription' },
    { key:'wearable', label:'Smartwatch vinculado y conectado', ok: state.wearable.linked && state.wearable.connection === 'connected', fix:'/device' },
    { key:'location', label:'Ubicación precisa/GPS concedido', ok: state.permissions.location && state.gpsReady, fix:'/permissions' },
    { key:'background', label:'Servicio en segundo plano permitido', ok: state.permissions.background, fix:'/permissions' },
    { key:'bluetooth', label:'Bluetooth activo para Wear OS', ok: state.permissions.bluetooth, fix:'/permissions' },
    { key:'contacts', label:'Contactos activos disponibles', ok: activeContacts().length > 0, fix:'/contacts' },
    { key:'sqlite', label:'Cache local sincronizada para modo interno offline', ok: state.sqliteReady, fix:'/sync' },
    { key:'chat', label:'Chat interno privado habilitado', ok: state.internalChatReady, fix:'/chat' },
    { key:'battery', label:'Batería del wearable mayor a 20%', ok: state.wearable.battery > 20, fix:'/device' }
  ];
  return checks;
}
function canStartTrip() { return tripChecks().every(c => c.ok); }
function tripElapsedSeconds() { if (!state.trip?.startedAt) return 0; return Math.max(0, Math.floor((Date.now() - Number(state.trip.startedAt)) / 1000)); }
function tripElapsedLabel() {
  const sec = tripElapsedSeconds();
  const m = String(Math.floor(sec / 60)).padStart(2,'0');
  const s = String(sec % 60).padStart(2,'0');
  return `${m}:${s}`;
}
function tripTelemetry() {
  const sec = tripElapsedSeconds();
  const paused = state.trip?.paused;
  const speed = paused ? 0 : 28 + ((sec * 7) % 54);
  const g = paused ? '0.0G' : `${(1 + ((sec % 6) / 10)).toFixed(1)}G`;
  const heart = plan().telemetry ? (paused ? 76 : 84 + (sec % 38)) + ' bpm' : 'N/A';
  const noise = plan().telemetry ? (paused ? 39 : 48 + (sec % 32)) + 'dB' : 'N/A';
  const distance = Math.max(0.1, (sec * Math.max(speed, 18) / 3600)).toFixed(2);
  const risk = speed > 72 ? 'Medio' : speed > 55 ? 'Bajo+' : 'Bajo';
  const gps = sec % 2 === 0 ? '±4 m' : '±6 m';
  return { speed, g, heart, noise, distance, risk, gps };
}

function screenTripStart() {
  const checks = tripChecks();
  const ready = checks.every(c => c.ok);
  const selected = selectedRoute();
  return shell({ title: 'Viaje desde wearable', subtitle: 'Solo visualización móvil', bottom: false, back: '/home', content: `
    <div class="section-title"><h2>El móvil ya no inicia viajes</h2><p>En la versión final, el usuario inicia la conducción desde el wearable. El teléfono recibe el evento, prepara GPS, chat interno y seguimiento de ruta.</p></div>
    <div class="card ${ready ? 'success' : 'warning'}">
      <div class="row between"><div><h2>${ready ? 'Listo para recibir viaje' : 'Protección limitada'}</h2><p>${ready ? 'Cuando el reloj inicie viaje, esta app mostrará la ruta y el monitoreo.' : 'Corrige pendientes antes de depender del seguimiento.'}</p></div>${chip(ready ? 'Esperando Wear OS' : 'Limitado', ready ? 'success' : 'warning')}</div>
    </div>
    <div class="card soft"><h3>Ruta seleccionada para hoy</h3><p>${selected ? selected.label + ' · ' + selected.note : 'No hay ruta seleccionada.'}</p><div class="row wrap"><button class="btn small" onclick="go('/routes')">Elegir ruta</button><button class="btn small" onclick="go('/templates')">Mensajes precargados</button></div></div>
    <div class="section-title"><h2>Checklist realista</h2><p>Estas validaciones se revisan cuando el wearable envía el inicio de viaje.</p></div>
    ${checks.map(c => `<div class="list-item row between"><div><strong>${c.label}</strong><p>${c.ok ? 'Validado correctamente.' : 'Requiere atención.'}</p></div>${c.ok ? chip('OK','success') : `<button class="btn small warning" onclick="go('${c.fix}')">Corregir</button>`}</div>`).join('')}
    <button class="btn primary block" onclick="go('/device')">Ir al wearable</button>
    <button class="btn ghost block" onclick="go('/home')">Volver al inicio</button>` });
}

function screenTripActive() {
  if (!state.trip.active) return screenNoActiveTrip();
  const live = tripTelemetry();
  return shell({ title: 'Viaje activo', subtitle: state.trip.paused ? 'Monitoreo pausado' : 'Inicio desde wearable', bottom: false, back: '/home', content: `
    <div class="drive-header ${state.trip.paused ? 'paused' : ''}">
      <div class="row between"><div><strong>${state.trip.routeName}</strong><span>${state.trip.purpose} · iniciado ${state.trip.startedLabel}</span></div>${chip(state.trip.paused ? 'Pausado' : 'Activo', state.trip.paused ? 'warning' : 'success')}</div>
      <div class="speedometer"><strong>${live.speed}</strong><span>km/h</span></div>
      <div class="route-strip"><span></span><span></span><span></span><span></span></div>
    </div>
    <div class="grid-3">
      ${kv('Tiempo', tripElapsedLabel())}
      ${kv('Distancia', live.distance + ' km')}
      ${kv('GPS', live.gps)}
    </div>
    <div class="section-title"><h2>Telemetría del viaje</h2><p>Valores simulados como los mostraría la app final durante conducción.</p></div>
    <div class="sensor-grid">
      <div class="sensor"><b>Fuerza G</b><strong>${live.g}</strong><span>Base normal</span></div>
      <div class="sensor"><b>Ruido</b><strong>${live.noise}</strong><span>${plan().telemetry ? 'Micrófono activo' : 'Bloqueado por plan'}</span></div>
      <div class="sensor"><b>Ritmo</b><strong>${live.heart}</strong><span>${plan().telemetry ? 'PPG activo' : 'Bloqueado por plan'}</span></div>
      <div class="sensor"><b>Riesgo</b><strong>${live.risk}</strong><span>Modelo Edge AI</span></div>
    </div>
    <div class="card soft">
      <div class="row between"><strong>Comunicación de emergencia</strong>${chip(state.networkOnline ? 'Chat interno online' : 'Chat interno offline', state.networkOnline ? 'success' : 'warning')}</div>
      <div class="row wrap" style="margin-top:12px">${activeContacts().map(c=>chip(c.name, 'info')).join('')}</div>
    </div>
    <div class="row wrap">
      <button class="btn small" onclick="pauseTrip()">${state.trip.paused ? 'Reanudar' : 'Pausar'}</button>
      <button class="btn small" onclick="toggleNetwork()">${state.networkOnline ? 'Simular túnel/offline' : 'Recuperar internet'}</button>
      <button class="btn small warning" onclick="tripBump()">Bache fuerte</button>
      <button class="btn small danger" onclick="go('/sos-countdown')">Choque detectado</button>
    </div>
    <button class="btn block" onclick="go('/chat')">Abrir chat interno</button>
    <button class="btn success block" onclick="finishTrip()">Finalizar viaje</button>` });
}

function screenTripSummary() {
  const last = state.tripHistory[0];
  if (!last) return shell({ title: 'Resumen viaje', subtitle: 'Sin registros', back: '/home', content: `<div class="card warning"><h2>No hay viaje finalizado</h2><p>Cuando el wearable finalice un viaje, el resumen aparecerá aquí.</p></div><button class="btn primary block" onclick="go('/routes')">Preparar ruta frecuente</button>` });
  return shell({ title: 'Resumen viaje', subtitle: last.endedAt, back: '/home', content: `
    <div class="card success"><h2>Viaje finalizado</h2><p>${last.routeName}</p><div class="grid-2" style="margin-top:14px">${kv('Duración', last.duration)}${kv('Distancia', last.distance + ' km')}${kv('Vel. promedio', last.avgSpeed + ' km/h')}${kv('Riesgo', last.risk)}</div></div>
    <div class="timeline card soft">
      <div class="timeline-item"><div class="dot"></div><div><strong>Permiso iniciado</strong><p>Se armó GPS, Wear OS, SQLite y servicio en segundo plano.</p></div></div>
      <div class="timeline-item"><div class="dot"></div><div><strong>Monitoreo de ruta</strong><p>Se registró telemetría simulada y checkpoints locales.</p></div></div>
      <div class="timeline-item"><div class="dot"></div><div><strong>Sincronización</strong><p>${state.networkOnline ? 'Resumen enviado a nube.' : 'Resumen pendiente de sincronizar por falta de internet.'}</p></div></div>
    </div>
    <button class="btn primary block" onclick="go('/routes')">Preparar ruta frecuente</button>
    <button class="btn block" onclick="go('/incidents')">Ver historial</button>` });
}

function selectedRoute() {
  return state.frequentRoutes.find(r => r.id === state.routeDraft.selectedRouteId) || state.frequentRoutes[0] || null;
}

function screenNoActiveTrip() {
  const selected = selectedRoute();
  return shell({ title: 'Sin viaje activo', subtitle: 'Esperando wearable', bottom: false, back: '/home', content: `
    <div class="card warning"><h2>No hay viaje activo</h2><p>El inicio del viaje se realiza desde el wearable. En el móvil puedes elegir la ruta frecuente y avisarla por chat interno.</p></div>
    <div class="card soft"><h3>Ruta preparada</h3><p>${selected ? selected.label : 'Sin ruta seleccionada'}</p><div class="row wrap"><button class="btn small" onclick="go('/routes')">Cambiar ruta</button><button class="btn small" onclick="go('/chat')">Chat interno</button></div></div>
    <button class="btn primary block" onclick="go('/device')">Ver estado del wearable</button>` });
}

function screenFrequentRoutes() {
  const selected = selectedRoute();
  return shell({ title: 'Rutas frecuentes', subtitle: 'Etiquetas personales', back: '/home', actions: `<button class="icon-btn" onclick="go('/chat')">💬</button>`, content: `
    <div class="section-title"><h2>Elige qué ruta tomarás hoy</h2><p>Este apartado ya no depende del chat: desde aquí puedes crear, etiquetar, seleccionar y avisar la ruta que tomarás ese día.</p></div>
    <div class="card route-today-card ${state.routeDraft.todayRouteShared ? 'success' : 'soft'}">
      <div class="row between"><div><h3>Ruta preparada para hoy</h3><p>${selected ? selected.label : 'Sin ruta seleccionada'}</p></div>${chip(state.routeDraft.todayRouteShared ? 'Avisada' : 'Pendiente', state.routeDraft.todayRouteShared ? 'success' : 'warning')}</div>
      <p class="subtle">Último aviso interno: ${state.routeDraft.lastSharedAt}</p>
      <div class="row wrap" style="margin-top:12px"><button class="btn small primary" onclick="sendTemplateToMonitors(801)">Avisar inicio de ruta</button><button class="btn small" onclick="go('/chat')">Ver conversación</button></div>
    </div>
    ${state.frequentRoutes.map(r => `<div class="list-item route-card ${selected && selected.id === r.id ? 'selected' : ''}"><div class="row between"><div><strong>${r.name}</strong><p>${r.label}</p><span class="subtle">${r.origin} → ${r.destination} · ${r.note} · usada ${r.lastUsed}</span></div>${selected && selected.id === r.id ? chip('Hoy','primary') : `<button class="btn small" onclick="selectRouteToday(${r.id}, false)">Usar</button>`}</div><div class="row wrap" style="margin-top:10px"><button class="btn small" onclick="selectRouteToday(${r.id}, true)">Usar y avisar</button><button class="btn small ghost" onclick="toast('Edición visual simulada')">Editar</button></div></div>`).join('')}
    <div class="section-title"><h2>Agregar ruta frecuente</h2><p>Agrega rutas por etiqueta para reutilizarlas sin escribirlas cada día.</p></div>
    <div class="card"><div class="form-grid">
      ${field('Nombre de la ruta', '', 'routeName')}
      ${field('Etiqueta visible', 'Casa → Destino', 'routeLabel')}
      ${field('Origen', 'Casa', 'routeOrigin')}
      ${field('Destino', '', 'routeDestination')}
      <label><span class="label">Nota</span><textarea id="routeNote" class="textarea">Ruta frecuente guardada para aviso interno.</textarea></label>
      <button class="btn primary block" onclick="addFrequentRoute()">Guardar ruta</button>
    </div></div>
    <div class="card soft"><h3>Mensaje libre con ruta</h3><p>También puedes escribir un mensaje propio y enviarlo a tu red interna.</p><textarea id="routeCustomMsg" class="textarea">Hoy tomaré ${selected ? selected.label : 'la ruta seleccionada'}; estaré pendiente desde el wearable.</textarea><button class="btn block" onclick="sendRouteCustomMessage()">Enviar mensaje personalizado</button></div>
    <button class="btn block" onclick="go('/templates')">Ver mensajes precargados</button>` });
}

function chatBubbleClass(msg) {
  if (msg.from === state.user.name || msg.from === state.user.username || msg.type === 'me') return 'me';
  if (msg.from === 'Sistema Impact.X') return 'system';
  return 'reply';
}

function chatRecipientOptions() {
  const active = state.monitors.filter(m => m.status === 'Activo');
  return `<option>Red familiar</option>` + active.map(m => `<option>${m.name}</option>`).join('');
}

function screenInternalChat() {
  const selected = selectedRoute();
  const active = state.monitors.filter(m => m.status === 'Activo');
  return shell({ title: 'Chat interno', subtitle: 'Mensajes dentro de Impact.X', back: '/home', actions: `<button class="icon-btn" onclick="go('/routes')">⌖</button>`, content: `
    <div class="chat-hero-card">
      <div class="row between"><div><h2>Chat interno Impact.X</h2><p>Este es el único canal oficial: aquí se envían rutas, avisos, respuestas de familiares y alertas SOS.</p></div>${chip('Sin SMS/WhatsApp/email','success')}</div>
      <div class="chat-status-row"><span>● ${active.length} monitores activos</span><span>● ${state.internalMessages.length} mensajes</span><span>● ${state.networkOnline ? 'Online' : 'Pendiente offline'}</span></div>
    </div>
    <div class="quick-grid">
      <button class="quick-action" onclick="sendTemplateToMonitors(801)"><b>🚗</b><span>Mandar ruta de hoy</span></button>
      <button class="quick-action" onclick="go('/templates')"><b>⚡</b><span>Mensajes precargados</span></button>
      <button class="quick-action" onclick="go('/invite-user')"><b>👤</b><span>Invitar usuario/ID</span></button>
      <button class="quick-action" onclick="go('/routes')"><b>⌖</b><span>Rutas frecuentes</span></button>
    </div>
    <div class="section-title"><h2>Red familiar</h2><p>Toca “Invitar usuario/ID” para agregar personas usando su identificador único dentro de la app.</p></div>
    ${state.monitors.map(m => `<div class="list-item row between"><div class="row"><div class="avatar">${initials(m.name)}</div><div><strong>${m.name}</strong><p>@${m.username || 'usuario'} · ${m.appUserId || m.token}</p></div></div>${chip(m.status, statusBadge(m.status))}</div>`).join('')}
    <div class="section-title"><h2>Conversación</h2><p>Las respuestas se simulan automáticamente para mostrar cómo se vería la conversación real.</p></div>
    <div class="chat-panel friendly-chat">${state.internalMessages.map(msg => `<div class="chat-bubble ${chatBubbleClass(msg)}"><strong>${msg.title}</strong><p>${msg.body}</p><span>${msg.from} → ${msg.to} · ${msg.time}</span></div>`).join('')}</div>
    <div class="card composer-card"><h3>Enviar mensaje personalizado</h3><p class="subtle">Escribe cualquier mensaje para tu red o para un contacto activo.</p><label><span class="label">Destinatario</span><select id="chatRecipient" class="select">${chatRecipientOptions()}</select></label><label><span class="label">Mensaje</span><textarea id="customChatMsg" class="textarea">${state.chatDraft.lastCustomBody}</textarea></label><button class="btn primary block" onclick="sendCustomInternalMessage()">Enviar mensaje interno</button></div>
    <div class="card"><h3>Mensaje rápido</h3><p class="subtle">Ruta actual: ${selected ? selected.label : 'Sin ruta'}</p><div class="row wrap">${state.messageTemplates.slice(0,4).map(t => `<button class="btn small" onclick="sendTemplateToMonitors(${t.id})">${t.title}</button>`).join('')}</div></div>
    <button class="btn block" onclick="simulateIncomingReply()">Simular respuesta recibida</button>
    <button class="btn primary block" onclick="go('/templates')">Administrar mensajes precargados</button>` });
}

function screenMessageTemplates() {
  const selected = selectedRoute();
  return shell({ title: 'Mensajes precargados', subtitle: 'Chat interno', back: '/chat', content: `
    <div class="section-title"><h2>Plantillas de comunicación</h2><p>Sirven para avisar inicio de ruta, cambios, llegada o incidentes sin escribir desde cero. Después de enviarlos, la maqueta genera respuestas simuladas de tu red.</p></div>
    ${state.messageTemplates.map(t => `<div class="list-item"><div class="row between"><div><strong>${t.title}</strong><p>${t.body.replace('{ruta}', selected ? selected.label : 'ruta seleccionada')}</p></div><button class="btn small primary" onclick="sendTemplateToMonitors(${t.id})">Enviar</button></div></div>`).join('')}
    <div class="card composer-card"><h3>Mensaje libre</h3><p class="subtle">Además de los mensajes precargados, el usuario puede mandar lo que necesite.</p><label><span class="label">Destinatario</span><select id="templateRecipient" class="select">${chatRecipientOptions()}</select></label><textarea id="templateCustomMsg" class="textarea">Voy en camino por ${selected ? selected.label : 'mi ruta de hoy'}.</textarea><button class="btn primary block" onclick="sendCustomTemplateMessage()">Enviar mensaje personalizado</button></div>
    <div class="card soft"><h3>Cómo funcionaría en final</h3><p>El wearable manda el evento de inicio de conducción; el móvil adjunta la ruta frecuente elegida y la publica como mensaje interno para monitores activos.</p></div>` });
}

function screenInviteUser() {
  return shell({ title: 'Invitar usuario', subtitle: 'Usuario o ID único Impact.X', back: '/contacts', content: `
    <div class="section-title"><h2>Agregar a mi red familiar</h2><p>Busca a la persona por su <strong>@usuario</strong> o por su <strong>ID Impact.X</strong>. La solicitud queda pendiente hasta que la acepte dentro de la app.</p></div>
    <div class="card identity-card"><div class="row between"><div><strong>Tu identidad interna</strong><p>@${state.user.username} · ${state.user.appId}</p></div>${chip('Único','primary')}</div><p class="subtle">Este ID se puede compartir para que otros te agreguen sin usar SMS, WhatsApp ni correo.</p></div>
    <div class="card"><div class="form-grid">
      <label><span class="label">Usuario o ID de la persona</span><input id="inviteIdentity" class="input" value="maria_tejeda" placeholder="maria_tejeda o IX-MAR-7731" /></label>
      <label><span class="label">Mensaje de invitación</span><textarea id="inviteMsg" class="textarea">Quiero agregarte a mi red familiar de Impact.X para compartirte rutas y alertas internas.</textarea></label>
      <button class="btn primary block" onclick="sendUserInvite()">Enviar solicitud interna</button>
      <button class="btn block" onclick="fillRandomDirectoryUser()">Probar con usuario demo</button>
    </div></div>
    <div class="section-title"><h2>Usuarios demo disponibles</h2><p>Sirven para probar la búsqueda por usuario o ID.</p></div>
    ${state.appDirectory.map(u => `<div class="list-item row between"><div><strong>${u.name}</strong><p>@${u.username} · ${u.appUserId}</p></div><button class="btn small" onclick="prefillInvite('${u.username}')">Usar</button></div>`).join('')}
    <div class="card soft"><h3>Invitar a alguien nuevo</h3><p>Si la persona aún no tiene cuenta, en una versión final compartirías tu código <strong>${state.user.inviteCode}</strong>. Al crear cuenta, esa persona ingresaría el código y aparecería como solicitud pendiente.</p></div>` });
}

function screenDevice() {
  return shell({ title: 'Dispositivo', subtitle: state.wearable.model, actions: `<button class="icon-btn" onclick="syncNow()">↻</button>`, content: `
    <div class="card">
      <div class="row between"><div><h2>${state.wearable.model}</h2><p>Versión ${state.wearable.version} · ${state.wearable.lastSync}</p></div>${chip(state.wearable.connection === 'connected' ? 'Conectado' : 'Desconectado', state.wearable.connection === 'connected' ? 'success' : 'danger')}</div>
      <div style="margin:14px 0"><div class="progress"><span style="width:${state.wearable.battery}%"></span></div><p class="subtle">Batería ${state.wearable.battery}%</p></div>
      <div class="grid-2">${kv('Calibración', state.wearable.calibration + '%')}${kv('Sensores', plan().sensors.length)}</div>
    </div>
    <div class="section-title"><h2>Sensores disponibles</h2><p>Dependen del hardware y del plan activo.</p></div>
    ${plan().sensors.map(s => `<div class="list-item row between"><strong>${s}</strong>${chip('Activo', 'success')}</div>`).join('')}
    <button class="btn block" onclick="toggleWearableConnection()">${state.wearable.connection === 'connected' ? 'Simular desconexión' : 'Reconectar reloj'}</button>
    <button class="btn primary block" onclick="receiveWearableTripStart()">Recibir inicio de viaje desde wearable</button>
    <button class="btn block" onclick="go('/wearable-code')">Sincronizar por código</button>
    <button class="btn block" onclick="go('/calibration')">Recalibrar sensores</button>
    <button class="btn danger block" onclick="unlinkWearable()">Desvincular dispositivo</button>` });
}
function screenContacts() {
  return shell({ title: 'Contactos', subtitle: `${activeContacts().length}/${plan().contactsLimit} activos`, actions: `<button class="icon-btn" onclick="go('/monitors')">👥</button>`, content: `
    <div class="card soft">
      <div class="row between"><div><strong>Red interna</strong><p>Contactos/familia asociados al chat privado de Impact.X.</p></div>${chip(state.sqliteReady ? 'Sincronizado' : 'Pendiente', state.sqliteReady ? 'success' : 'warning')}</div>
    </div>
    ${contactList(true)}
    <button class="btn primary block" onclick="go('/invite-user')">Invitar por usuario/ID</button>
    <button class="btn block" onclick="newContactFlow()">Agregar contacto manual</button>
    <button class="btn block" onclick="go('/routes')">Preparar rutas frecuentes</button>
    <button class="btn block" onclick="go('/monitors')">Gestionar red familiar</button>
    <button class="btn block" onclick="go('/chat')">Abrir chat interno</button>` });
}
function contactList(clickable = true) {
  if (!state.contacts.length) return `<div class="card warning"><h3>Sin contactos</h3><p>Agrega al menos un contacto para poder enviar SOS.</p></div>`;
  return state.contacts.map(c => `
    <div class="list-item" ${clickable ? `onclick="go('/contact/${c.id}')"` : ''}>
      <div class="row between">
        <div class="row grow"><div class="avatar">${initials(c.name)}</div><div class="grow"><strong>${c.name}</strong><p>${c.relation} · @${c.username || 'sin_usuario'} · ${c.appUserId || 'sin ID'}</p></div></div>
        ${chip(c.status, statusBadge(c.status))}
      </div>
      <div class="row wrap" style="margin-top:10px">${chip(c.priority, 'primary')}${chip(c.channel, 'info')}${c.monitorId ? chip('Monitor', 'success') : chip('Sin monitor', 'warning')}</div>
    </div>`).join('');
}
function screenContactDetail(id) {
  const c = state.contacts.find(x => x.id === id);
  if (!c) return screen404();
  return shell({ title: 'Detalle contacto', subtitle: c.name, back: '/contacts', content: `
    <div class="card">
      <div class="row"><div class="avatar">${initials(c.name)}</div><div><h2>${c.name}</h2><p>${c.relation} · ${c.priority}</p></div></div>
      <div class="grid-2" style="margin-top:14px">${kv('Estado', c.status)}${kv('Canal', 'Chat interno')}${kv('Usuario', '@' + (c.username || 'sin_usuario'))}${kv('ID', c.appUserId || 'Sin ID')}${kv('Teléfono', c.phone)}${kv('Familia', c.monitorId ? 'Sí' : 'No')}</div>
      <p style="margin-top:12px">${c.notes || 'Sin notas adicionales.'}</p>
    </div>
    <button class="btn block" onclick="makePrincipal(${c.id})">Marcar principal</button>
    <button class="btn primary block" onclick="createMonitorFromContact(${c.id})">Enviar solicitud interna</button><button class="btn block" onclick="sendDirectContactMessage(${c.id})">Enviar mensaje personalizado</button>
    <button class="btn danger block" onclick="deleteContact(${c.id})">Eliminar contacto</button>` });
}
function screenContactForm() {
  return shell({ title: 'Nuevo contacto', subtitle: 'Contacto + SQLite', bottom: state.onboardingComplete, back: state.onboardingComplete ? '/contacts' : '/contacts-setup', content: `
    <div class="section-title"><h2>Agregar contacto</h2><p>Si el plan llega al límite, el contacto se bloqueará hasta subir de plan.</p></div>
    <div class="card"><div class="form-grid">
      ${field('Usuario o ID Impact.X', '', 'contactIdentity')}
      ${field('Nombre', '', 'contactName')}
      ${field('Relación', 'Familiar', 'contactRelation')}
      ${field('Teléfono opcional', '+52 ', 'contactPhone')}
      <label><span class="label">Canal de alerta</span><input id="contactChannel" class="input" value="Chat interno Impact.X" disabled /></label>
      <button class="btn primary block" onclick="saveNewContact()">Guardar contacto</button>
      <button class="btn block" onclick="go('/invite-user')">Mejor invitar por usuario/ID</button>
    </div></div>` });
}
function screenPlanLimit() {
  return shell({ title: 'Límite de plan', subtitle: plan().name, back: '/contacts', content: `
    <div class="card warning"><h2>Límite alcanzado</h2><p>Tu plan ${plan().name} permite ${plan().contactsLimit} contactos activos. Puedes eliminar uno, suspenderlo o subir de plan.</p></div>
    <div class="grid-2">${kv('Activos', activeContacts().length)}${kv('Límite', plan().contactsLimit)}</div>
    <button class="btn primary block" onclick="go('/change-plan')">Actualizar plan</button>
    <button class="btn block" onclick="go('/contacts')">Gestionar contactos</button>` });
}
function screenMonitors() {
  return shell({ title: 'Monitores', subtitle: 'Invitaciones y permisos', back: '/contacts', content: `
    <div class="card soft"><p>Los monitores funcionan como una red familiar interna: aceptan solicitud, reciben rutas, chat y alertas dentro de Impact.X.</p></div>
    ${monitorList(true)}
    <button class="btn primary block" onclick="go('/invite-user')">Invitar por usuario/ID</button><button class="btn block" onclick="go('/monitor-form')">Invitación manual demo</button>` });
}
function monitorList(clickable = true) {
  if (!state.monitors.length) return `<div class="card warning"><h3>Sin monitores</h3><p>Genera una invitación para un familiar.</p></div>`;
  return state.monitors.map(m => `
    <div class="list-item" ${clickable ? `onclick="go('/monitor/${m.id}')"` : ''}>
      <div class="row between"><div class="row grow"><div class="avatar">${initials(m.name)}</div><div><strong>${m.name}</strong><p>@${m.username || 'usuario'} · ${m.appUserId || m.token}</p></div></div>${chip(m.status, statusBadge(m.status))}</div>
    </div>`).join('');
}
function screenMonitorForm() {
  return shell({ title: 'Nueva invitación', subtitle: 'Solicitud interna', bottom: state.onboardingComplete, back: state.onboardingComplete ? '/monitors' : '/monitoring-setup', content: `
    <div class="section-title"><h2>Invitar monitor</h2><p>Envía una solicitud interna para que el familiar se una a tu red, como agregar amigo o unirse a una familia compartida.</p></div>
    <div class="card"><div class="form-grid">
      ${field('Usuario o ID Impact.X', '', 'monitorIdentity')}
      ${field('Nombre del monitor', '', 'monitorName')}
      ${field('Teléfono opcional', '+52 ', 'monitorPhone')}
      <button class="btn primary block" onclick="saveNewMonitor()">Generar solicitud interna</button>
      <button class="btn block" onclick="go('/invite-user')">Buscar usuario demo</button>
    </div></div>` });
}
function screenMonitorDetail(id) {
  const m = state.monitors.find(x => x.id === id);
  if (!m) return screen404();
  return shell({ title: 'Detalle monitor', subtitle: m.name, back: '/monitors', content: `
    <div class="card">
      <div class="row between"><div><h2>${m.name}</h2><p>@${m.username || 'usuario'} · ${m.appUserId || m.token}</p></div>${chip(m.status, statusBadge(m.status))}</div>
      <div class="grid-2" style="margin-top:14px">${kv('Token', m.token)}${kv('Usuario', '@' + (m.username || 'usuario'))}${kv('ID', m.appUserId || 'Sin ID')}${kv('Aceptado', m.acceptedAt)}${kv('Invitado', m.invitedAt)}${kv('Expira', m.expiresAt)}</div>
    </div>
    <div class="card soft"><h3>Permisos</h3><p>Recibir SOS, ver ubicación durante incidente, recibir actualizaciones del cierre.</p></div>
    <button class="btn block" onclick="shareMonitor(${m.id})">Enviar solicitud interna</button>
    <button class="btn success block" onclick="setMonitorStatus(${m.id}, 'Activo')">Restaurar / activar</button>
    <button class="btn warning block" onclick="setMonitorStatus(${m.id}, 'Pendiente')">Reenviar invitación</button>
    <button class="btn danger block" onclick="setMonitorStatus(${m.id}, 'Revocado')">Revocar acceso</button>` });
}
function screenSubscription() {
  return shell({ title: 'Suscripción', subtitle: `${plan().name} · Activa`, back: '/profile', content: `
    <div class="card">
      <div class="row between"><div><h2>${plan().name}</h2><p>${plan().description}</p></div>${chip('Activa', 'success')}</div>
      <div class="grid-2" style="margin-top:14px">${kv('Precio', plan().price)}${kv('Días restantes', plan().days)}${kv('Contactos', `${activeContacts().length}/${plan().contactsLimit}`)}${kv('Bypass', plan().bypass ? 'Sí' : 'No')}</div>
    </div>
    <div class="section-title"><h2>Funciones activas</h2></div>
    ${plan().sensors.map(s => `<div class="list-item row between"><strong>${s}</strong>${chip('Activo', 'success')}</div>`).join('')}
    <button class="btn primary block" onclick="go('/change-plan')">Cambiar plan</button>
    <button class="btn block" onclick="go('/payment')">Renovar / pagar</button>
    <button class="btn danger block" onclick="go('/subscription-expired')">Simular vencimiento</button>` });
}
function screenPayment() {
  return shell({ title: 'Pago', subtitle: 'Simulación de suscripción', back: '/subscription', content: `
    <div class="card"><h2>Confirmar pago</h2><p>Esta pantalla solo simula el flujo. No procesa pagos reales.</p><div class="grid-2" style="margin-top:14px">${kv('Plan', plan().name)}${kv('Total', plan().price)}</div></div>
    <div class="card"><div class="form-grid">${field('Tarjeta demo', '4242 4242 4242 4242', 'card')}${field('Titular', state.user.name, 'cardName')}<button class="btn primary block" onclick="payDemo()">Confirmar pago</button></div></div>` });
}
function screenExpired() {
  return shell({ title: 'Plan vencido', subtitle: 'Acceso limitado', bottom: false, back: '/subscription', content: `
    <div class="hero-mobile">
      <div class="card danger"><h2>Suscripción vencida</h2><p>La protección queda limitada: contactos y alertas pueden no sincronizar con nube.</p></div>
      <button class="btn primary block" onclick="go('/payment')">Renovar ahora</button>
      <button class="btn block" onclick="go('/change-plan')">Ver planes</button>
      <button class="btn ghost block" onclick="logoutDemo()">Cerrar sesión</button>
    </div>` });
}
function screenIncidents() {
  return shell({ title: 'Incidentes', subtitle: 'Historial móvil/nube', actions: `<button class="icon-btn" onclick="createDemoIncident()">＋</button>`, content: `
    <div class="card soft"><div class="row between"><strong>Historial</strong>${chip(plan().maps ? 'Mapas premium' : 'Mapas bloqueados', plan().maps ? 'success' : 'warning')}</div></div>
    ${state.incidents.map(i => incidentItem(i)).join('')}
    <button class="btn block" onclick="createDemoIncident()">Simular incidente registrado</button>` });
}
function incidentItem(i) {
  return `<div class="list-item" onclick="go('/incident/${i.id}')"><div class="row between"><div><strong>${i.type}</strong><p>${i.date} ${i.time} · ${i.location}</p></div>${chip(i.severity, i.severity === 'Alta' ? 'danger' : i.severity === 'Media' ? 'warning' : 'info')}</div><div class="row wrap" style="margin-top:10px">${chip(i.status, statusBadge(i.status))}${chip(i.gforce, 'primary')}${chip(i.channel, 'info')}</div></div>`;
}
function screenIncidentDetail(id) {
  const i = state.incidents.find(x => x.id === id);
  if (!i) return screen404();
  return shell({ title: 'Detalle incidente', subtitle: `${i.date} · ${i.time}`, back: '/incidents', content: `
    <div class="card"><div class="row between"><div><h2>${i.type}</h2><p>${i.location}</p></div>${chip(i.status, statusBadge(i.status))}</div><div class="grid-2" style="margin-top:14px">${kv('Severidad', i.severity)}${kv('Fuerza G', i.gforce)}${kv('Ruido', i.decibels)}${kv('Ritmo', i.heart)}${kv('Canal', i.channel)}${kv('Respuesta', i.response)}</div><p style="margin-top:12px">${i.note}</p></div>
    <div class="timeline card soft">
      <div class="timeline-item"><div class="dot"></div><div><strong>Detección</strong><p>El wearable reportó evento al móvil.</p></div></div>
      <div class="timeline-item"><div class="dot"></div><div><strong>Validación</strong><p>La app revisó plan, sensores y contactos SQLite.</p></div></div>
      <div class="timeline-item"><div class="dot"></div><div><strong>Resultado</strong><p>${i.status} mediante canal ${i.channel}.</p></div></div>
    </div>
    <button class="btn primary block" onclick="go('/map/${i.id}')">Ver mapa</button>` });
}
function screenMap(id) {
  const i = state.incidents.find(x => x.id === id) || state.incidents[0];
  return shell({ title: 'Mapa incidente', subtitle: i.location, back: `/incident/${i.id}`, content: `
    ${plan().maps ? `<div class="map"></div><div class="card"><h3>${i.location}</h3><p>Coordenadas ${i.gps}. En app real abre Google Maps o Mapbox.</p></div>` : `<div class="card warning"><h2>Mapa histórico Premium</h2><p>El plan ${plan().name} bloquea mapas históricos. La ubicación de emergencia activa sí puede compartirse durante un SOS.</p></div>`}
    <button class="btn block" onclick="toast('Abriendo Google Maps simulado')">Abrir en Google Maps</button>
    <button class="btn block" onclick="toast('Ubicación copiada')">Compartir ubicación</button>` });
}
function screenNotifications() {
  return shell({ title: 'Notificaciones', subtitle: `${unreadCount()} sin leer`, back: '/home', content: `
    ${state.notifications.map(n => `<div class="list-item" onclick="markNotification(${n.id})"><div class="row between"><div><strong>${n.title}</strong><p>${n.body}</p></div>${chip(n.read ? 'Leída' : 'Nueva', n.read ? 'info' : 'warning')}</div></div>`).join('')}
    <button class="btn block" onclick="markAllNotifications()">Marcar todas como leídas</button>` });
}
function screenProfile() {
  return shell({ title: 'Perfil', subtitle: state.user.name, actions: `<button class="icon-btn" onclick="go('/settings')">⚙</button>`, content: `
    <div class="card"><div class="row"><div class="avatar">${initials(state.user.name)}</div><div><h2>${state.user.name}</h2><p>@${state.user.username} · ${state.user.appId}</p></div></div><div class="grid-2" style="margin-top:14px">${kv('Plan', plan().name)}${kv('Usuario', '@' + state.user.username)}${kv('ID', state.user.appId)}${kv('Ciudad', state.user.city)}${kv('Vehículo', state.vehicle.model)}${kv('Sangre', state.medical.blood)}${kv('Padecimiento', state.medical.condition)}${kv('Medicamento', state.medical.meds)}</div></div>
    <button class="btn block" onclick="go('/driver')">Editar datos personales</button>
    <button class="btn block" onclick="go('/vehicle')">Editar vehículo</button>
    <button class="btn block" onclick="go('/medical')">Editar datos médicos</button>
    <button class="btn primary block" onclick="go('/subscription')">Suscripción</button>
    <button class="btn block" onclick="go('/routes')">Rutas frecuentes</button>
    <button class="btn block" onclick="go('/invite-user')">Invitar por usuario/ID</button>
    <button class="btn block" onclick="go('/sync')">Sincronización local</button>
    <button class="btn danger block" onclick="logoutDemo()">Cerrar sesión</button>` });
}
function screenSettings() {
  const rows = [['Seguridad','Contraseña, 2FA y sesiones','/security'],['Privacidad y datos','Ubicación, telemetría y SQLite','/privacy'],['Sincronización','Nube, móvil y almacenamiento local','/sync'],['Ayuda y soporte','Guías de wearable, SOS y planes','/help']];
  return shell({ title: 'Configuración', subtitle: 'Preferencias móviles', back: '/profile', content: rows.map(([a,b,c]) => `<div class="list-item row between" onclick="go('${c}')"><div><strong>${a}</strong><p>${b}</p></div><span>›</span></div>`).join('') });
}
function screenSecurity() {
  return shell({ title: 'Seguridad', subtitle: 'Cuenta Impact.X', back: '/settings', content: `
    <div class="card"><h2>Protección de cuenta</h2><p>Simulación de opciones de seguridad.</p></div>
    <button class="btn block" onclick="toast('Contraseña actualizada en simulación')">Cambiar contraseña</button>
    <button class="btn block" onclick="toast('2FA activado en simulación')">Activar 2FA</button>
    <button class="btn danger block" onclick="toast('Sesiones remotas cerradas')">Cerrar otras sesiones</button>` });
}
function screenPrivacy() {
  return shell({ title: 'Privacidad', subtitle: 'Datos y permisos', back: '/settings', content: `
    <div class="card"><h2>Datos compartidos</h2><p>La ubicación y datos médicos solo se comparten durante un incidente activo.</p></div>
    ${['Compartir ubicación en emergencia','Compartir datos médicos con monitores','Permitir aprendizaje de IA Edge','Guardar contactos en SQLite','Borrar datos locales'].map((x,i)=>`<div class="list-item row between"><strong>${x}</strong><button class="btn small ${i===4?'danger':'success'}" onclick="toast('${x} actualizado')">${i===4?'Borrar':'Activo'}</button></div>`).join('')}` });
}
function screenSync() {
  return shell({ title: 'Sincronización', subtitle: 'Azure SQL + cache local', back: '/profile', content: `
    <div class="card"><div class="row between"><h2>Estado local</h2>${chip(state.networkOnline ? 'Online' : 'Offline', state.networkOnline ? 'success' : 'warning')}</div><p>La app mantiene cache de contactos, rutas y mensajes internos. Ya no manda notificaciones ni mensajes por SMS, WhatsApp o email; todo queda dentro de Impact.X.</p></div>
    <div class="grid-2">${kv('Red interna', activeContacts().length)}${kv('Pendientes nube', state.networkOnline ? 0 : 1)}${kv('Chat interno', state.internalChatReady ? 'Listo' : 'No')}${kv('Última sync', state.lastSync)}</div>
    <button class="btn primary block" onclick="syncNow()">Sincronizar ahora</button>
    <button class="btn block" onclick="toggleNetwork()">${state.networkOnline ? 'Simular sin internet' : 'Restaurar internet'}</button>
    <button class="btn warning block" onclick="go('/chat')">Probar chat interno</button>` });
}
function screenHelp() {
  return shell({ title: 'Ayuda', subtitle: 'Mapa completo de pantallas', back: '/settings', content: `
    <div class="card"><h2>Prototipo móvil</h2><p>Incluye pantallas de acceso, onboarding, protección activa, dispositivo, contactos, monitores, suscripción, incidentes, configuración, modo monitor y flujo SOS.</p></div>
    ${['Splash / bienvenida / login / registro','Planes / permisos / perfil / médicos / vehículo','Vincular reloj / permisos Wear OS / calibración','Contactos / familia / invitaciones internas','Inicio / dispositivo / rutas frecuentes / chat interno','SOS manual / alerta detectada / falsa alarma','Chat interno / alerta activa / cierre de incidente','Historial / detalle / mapa / cierre','Perfil / seguridad / privacidad / soporte','Modo monitor / alerta recibida'].map(x=>`<div class="list-item"><strong>${x}</strong></div>`).join('')}` });
}
function screenSosConfirm() {
  return shell({ title: 'SOS manual', subtitle: 'Confirmación crítica', bottom: false, back: '/home', content: `
    <div class="hero-mobile">
      <div class="card danger"><h2>¿Enviar SOS ahora?</h2><p>Se compartirá tu ubicación actual con ${activeContacts().length} contactos y monitores activos.</p></div>
      <button class="btn danger block" onclick="startManualSos()">Enviar SOS ahora</button>
      <button class="btn block" onclick="go('/home')">Cancelar</button>
    </div>` });
}
function screenSosCountdown() {
  return shell({ title: '¿Estás bien?', subtitle: 'Posible choque detectado', bottom: false, back: '/home', content: `
    <div class="section-title"><h2>Impacto detectado</h2><p>${state.activePlan === 'basico' ? 'Plan Básico: temporizador obligatorio antes de enviar SOS.' : 'Responde antes de que termine la cuenta regresiva.'}</p></div>
    <div class="countdown-ring" style="--p:75"><strong id="countVal">20</strong></div>
    <div class="sensor-grid">
      <div class="sensor"><b>Fuerza G</b><strong>7.6G</strong><span>Umbral alto</span></div>
      <div class="sensor"><b>Ruido</b><strong>${plan().telemetry ? '112dB' : 'N/A'}</strong><span>${plan().telemetry ? 'Impacto posible' : 'Bloqueado'}</span></div>
      <div class="sensor"><b>Ritmo</b><strong>${plan().telemetry ? '128 bpm' : 'N/A'}</strong><span>${plan().telemetry ? 'Elevado' : 'Bloqueado'}</span></div>
      <div class="sensor"><b>GPS</b><strong>Listo</strong><span>Ubicación fija</span></div>
    </div>
    <button class="btn success block" onclick="go('/false-alarm')">Estoy bien</button>
    <button class="btn danger block" onclick="sendSos()">Enviar SOS</button>` });
}
function screenFalseAlarm() {
  return shell({ title: 'Falsa alarma', subtitle: 'Cancelar alerta', bottom: false, back: '/home', content: `
    <div class="section-title"><h2>Registrar motivo</h2><p>Esto ayuda a mejorar la calibración del modelo en el reloj.</p></div>
    <div class="card"><div class="form-grid">
      <label><span class="label">Motivo</span><select id="falseReason" class="select"><option>Bache</option><option>Caída leve</option><option>Movimiento brusco</option><option>Prueba</option><option>Otro</option></select></label>
      <label><span class="label">Nota</span><textarea id="falseNote" class="textarea">El usuario confirmó que está bien.</textarea></label>
      <button class="btn primary block" onclick="saveFalseAlarm()">Guardar cancelación</button>
    </div></div>` });
}
function screenSendingAlert() {
  return shell({ title: 'Enviando alerta', subtitle: 'Canal interno', bottom: false, back: '/home', content: `
    <div class="section-title"><h2>SOS en proceso</h2><p>La app valida GPS, contactos/familia, ruta de hoy y chat interno disponible.</p></div>
    <div class="timeline card">
      <div class="timeline-item"><div class="dot"></div><div><strong>GPS obtenido</strong><p>20.0531, -99.3432</p></div></div>
      <div class="timeline-item"><div class="dot"></div><div><strong>Red interna validada</strong><p>${activeContacts().length} contactos/familia listos para recibir alerta dentro de Impact.X.</p></div></div>
      <div class="timeline-item"><div class="dot"></div><div><strong>Chat interno</strong><p>${state.networkOnline ? 'Mensaje enviado por backend y push interno.' : 'Mensaje guardado localmente y pendiente de sincronizar.'}</p></div></div>
    </div>
    <button class="btn primary block" onclick="finishSending()">Completar envío interno</button>
    <button class="btn block" onclick="go('/chat')">Ver chat interno</button>` });
}
function screenOfflineSms() {
  return shell({ title: 'Modo interno offline', subtitle: 'Cache local + chat', bottom: false, back: '/home', content: `
    <div class="card warning"><h2>Sin conexión HTTP</h2><p>No se usan SMS, WhatsApp ni email. La app guarda el mensaje de emergencia en cache local y lo sincroniza al chat interno cuando vuelve internet.</p></div>
    ${activeContacts().map(c => `<div class="list-item row between"><div><strong>${c.name}</strong><p>${c.phone}</p></div>${chip('Pendiente interno', 'warning')}</div>`).join('')}
    <button class="btn primary block" onclick="finishSending(true)">Continuar a alerta activa</button>
    <button class="btn block" onclick="toggleNetwork()">Restaurar internet y sincronizar</button>` });
}
function screenActiveAlert() {
  const alert = state.currentAlert || makeAlertObject(false);
  return shell({ title: 'Alerta activa', subtitle: 'SOS compartiendo ubicación', bottom: false, content: `
    <div class="card danger"><div class="row between"><div><h2>SOS activo</h2><p>${alert.location}</p></div>${chip(alert.channel, 'warning')}</div></div>
    <div class="map"></div>
    <div class="grid-2" style="margin-top:12px">${kv('Tiempo', '02:18')}${kv('Severidad', alert.severity)}${kv('Fuerza G', alert.gforce)}${kv('Ritmo', alert.heart)}</div>
    <div class="section-title"><h2>Contactos notificados</h2></div>
    ${activeContacts().map(c => `<div class="list-item row between"><div><strong>${c.name}</strong><p>Chat interno · ${c.phone}</p></div>${chip('Notificado', 'success')}</div>`).join('')}
    <button class="btn block" onclick="go('/monitor-alert')">Ver cómo lo recibe un monitor</button>
    <button class="btn success block" onclick="go('/close-incident')">Marcar como atendido</button>` });
}
function screenCloseIncident() {
  return shell({ title: 'Cerrar incidente', subtitle: 'Guardar reporte', bottom: false, back: '/active-alert', content: `
    <div class="section-title"><h2>Resumen final</h2><p>Este reporte se sincronizará con web e historial móvil.</p></div>
    <div class="card"><div class="form-grid"><label><span class="label">¿Fue accidente real?</span><select id="realAccident" class="select"><option>Sí</option><option>No</option><option>Prueba</option></select></label><label><span class="label">Nota final</span><textarea id="closeNote" class="textarea">Alerta atendida por contacto principal.</textarea></label><button class="btn primary block" onclick="closeIncident()">Guardar y cerrar</button></div></div>` });
}
function screenInviteAccept() {
  return shell({ title: 'Invitación', subtitle: 'Modo monitor', bottom: false, back: '/welcome', content: `
    <div class="hero-mobile"><div class="card"><h2>Leonardo te invitó como monitor</h2><p>Recibirás mensajes internos, rutas compartidas y alertas SOS solo dentro de Impact.X.</p></div><div class="card soft"><p>Permisos: recibir SOS interno, ver ruta/ubicación durante incidente y marcar “voy en camino”.</p></div><button class="btn primary block" onclick="acceptMonitorInvite()">Aceptar invitación</button><button class="btn block" onclick="go('/welcome')">Rechazar</button></div>` });
}
function screenMonitorHome() {
  return shell({ title: 'Monitor', subtitle: 'Red de emergencia', bottom: false, actions: '', content: `
    <div class="card success"><h2>Monitor activo</h2><p>Estás asociado a ${state.user.name}. Recibirás alertas y rutas únicamente dentro del chat interno.</p></div>
    <button class="btn danger block" onclick="go('/monitor-alert')">Simular alerta recibida</button>
    <button class="btn block" onclick="go('/invite-accept')">Ver permisos</button>
    <button class="btn ghost block" onclick="go('/welcome')">Salir de modo monitor</button>` });
}
function screenMonitorAlert() {
  const alert = state.currentAlert || makeAlertObject(false);
  return shell({ title: 'Alerta recibida', subtitle: state.user.name, bottom: false, back: '/monitor-home', content: `
    <div class="card danger"><h2>Emergencia de ${state.user.name}</h2><p>${alert.location} · ${alert.time}</p></div>
    <div class="map"></div>
    <div class="grid-2" style="margin-top:12px">${kv('Severidad', alert.severity)}${kv('Sangre', state.medical.blood)}${kv('Vehículo', state.vehicle.model)}${kv('Canal', 'Chat interno')}</div>
    <div class="card soft"><h3>Datos médicos</h3><p><strong>Padecimiento:</strong> ${state.medical.condition}<br><strong>Alergias:</strong> ${state.medical.allergies}<br><strong>Medicamento:</strong> ${state.medical.meds}<br><strong>Nota:</strong> ${state.medical.note}</p></div>
    <button class="btn danger block" onclick="toast('Llamando al conductor en simulación')">Llamar conductor</button>
    <button class="btn primary block" onclick="toast('Abriendo ruta en Maps')">Abrir ruta en Maps</button>
    <button class="btn success block" onclick="toast('Estado enviado: voy en camino')">Voy en camino</button>` });
}
function screen404() {
  return shell({ title: '404', subtitle: 'Pantalla no encontrada', back: '/home', content: `<div class="card warning"><h2>Ruta no encontrada</h2><p>Vuelve al inicio del prototipo móvil.</p></div><button class="btn primary block" onclick="go('/home')">Ir al inicio</button>` });
}

// Actions
function loginDemo() { const identity = document.getElementById('loginIdentity')?.value?.trim(); if (identity) state.user.lastLogin = identity; state.loggedIn = true; save(); toast(`Sesión iniciada como ${identity || state.user.username}`); go(state.onboardingComplete ? '/home' : '/permissions'); }
function registerDemo() { state.loggedIn = true; state.user.name = document.getElementById('regName').value || state.user.name; state.user.username = normalizeUsername(document.getElementById('regUsername')?.value || state.user.username); state.user.email = document.getElementById('regEmail').value || state.user.email; state.user.phone = document.getElementById('regPhone')?.value || state.user.phone; const invite = document.getElementById('regInviteCode')?.value?.trim(); if (invite) state.notifications.unshift({ id: Date.now(), title: 'Código de invitación detectado', body: `Solicitud familiar vinculada con código ${invite}.`, type: 'invite', read: false, route: '/chat' }); state.user.appId = generateUserId(state.user.username); save(); toast('Cuenta creada en simulación'); go('/plans'); }
function logoutDemo() { state.loggedIn = false; save(); go('/welcome'); }
function selectPlan(key, next) { state.activePlan = key; applyPlanLimits(); save(); toast(`Plan ${PLAN_RULES[key].name} seleccionado`); go(next); }
function applyPlanLimits() {
  const limit = plan().contactsLimit;
  let count = 0;
  state.contacts = state.contacts.map(c => {
    if (c.status === 'Eliminado') return c;
    count++;
    return { ...c, status: count <= limit ? 'Activo' : 'Suspendido por límite' };
  });
}
function togglePermission(key) { state.permissions[key] = !state.permissions[key]; save(); render(); }
function saveUserProfile(next) { state.user.name = document.getElementById('userName')?.value || state.user.name; state.user.username = normalizeUsername(document.getElementById('userUsername')?.value || state.user.username); state.user.appId = state.user.appId || generateUserId(state.user.username); state.user.phone = document.getElementById('userPhone')?.value || state.user.phone; state.user.city = document.getElementById('userCity')?.value || state.user.city; save(); go(next); }
function saveMedical(next) { state.medical.blood = document.getElementById('blood')?.value || state.medical.blood; state.medical.allergies = document.getElementById('allergies')?.value || state.medical.allergies; state.medical.condition = document.getElementById('condition')?.value || state.medical.condition; state.medical.meds = document.getElementById('meds')?.value || state.medical.meds; state.medical.note = document.getElementById('medNote')?.value || state.medical.note; save(); go(next); }
function saveVehicle(next) { state.vehicle.type = document.getElementById('vehicleType')?.value || state.vehicle.type; if (!FOUR_WHEEL_VEHICLE_TYPES.includes(state.vehicle.type)) state.vehicle.type = 'Auto'; state.vehicle.brand = document.getElementById('vehicleBrand')?.value || state.vehicle.brand; state.vehicle.model = document.getElementById('vehicleModel')?.value || state.vehicle.model; state.vehicle.year = document.getElementById('vehicleYear')?.value || state.vehicle.year; state.vehicle.avgSpeed = document.getElementById('avgSpeed')?.value || state.vehicle.avgSpeed; save(); go(next); }
function linkWearable() { go('/wearable-code'); }
function formatPairCode(value) { const raw = String(value || '').replace(/\D/g, '').slice(0, 6); return raw.length > 3 ? `${raw.slice(0,3)} ${raw.slice(3)}` : raw; }
function handlePairInput(input) { const pair = ensurePairing(); pair.enteredCode = input.value.replace(/\D/g, '').slice(0, 6); input.value = formatPairCode(pair.enteredCode); pair.step = pair.enteredCode.length ? 'idle' : 'idle'; pair.progress = pair.enteredCode.length ? 20 : 0; save(); }
function tapPairDigit(digit) { const pair = ensurePairing(); pair.enteredCode = String(pair.enteredCode || '').replace(/\D/g, '').slice(0, 6); if (pair.enteredCode.length < 6) pair.enteredCode += digit; pair.step = 'idle'; pair.progress = pair.enteredCode.length ? 20 : 0; save(); render(); }
function clearPairCode() { const pair = ensurePairing(); pair.enteredCode = String(pair.enteredCode || '').replace(/\D/g, '').slice(0, -1); pair.step = pair.enteredCode.length ? 'idle' : 'idle'; pair.progress = pair.enteredCode.length ? 20 : 0; save(); render(); }
function autoFillPairCode() { const pair = ensurePairing(); pair.enteredCode = pair.requiredCode; pair.step = 'idle'; pair.progress = 20; save(); render(); toast('Código demo ingresado'); }
function refreshPairCode() {
  const raw = String(Math.floor(100000 + Math.random() * 900000));
  const pair = ensurePairing();
  pair.requiredCode = raw;
  pair.visibleCode = `${raw.slice(0,3)} ${raw.slice(3)}`;
  pair.enteredCode = '';
  pair.step = 'idle';
  pair.progress = 0;
  pair.sessionId = `SES-WEAR-${today().replaceAll('-', '')}-${Math.floor(100 + Math.random()*900)}`;
  pair.trustToken = `IX-LINK-ANDROID-${Math.random().toString(36).slice(2,6).toUpperCase()}`;
  pair.lastAttempt = `${today()} ${nowTime()}`;
  state.wearable.linked = false;
  state.wearable.connection = 'disconnected';
  save();
  toast('Nuevo código generado en el wearable');
  go('/wearable-code');
}
function validatePairCode() {
  const pair = ensurePairing();
  const input = document.getElementById('wearCode');
  const entered = (input?.value || pair.enteredCode || '').replace(/\D/g, '').slice(0, 6);
  pair.enteredCode = entered;
  pair.lastAttempt = `${today()} ${nowTime()}`;
  if (entered !== pair.requiredCode) {
    pair.step = 'error';
    pair.progress = 20;
    state.wearable.linked = false;
    state.wearable.connection = 'disconnected';
    save();
    toast('Código incorrecto. Revisa el PIN del reloj.');
    render();
    return;
  }
  pair.step = 'validating'; pair.progress = 35; save(); render();
  setTimeout(() => { const p = ensurePairing(); p.step = 'handshake'; p.progress = 55; save(); render(); }, 450);
  setTimeout(() => { const p = ensurePairing(); p.step = 'permissions'; p.progress = 78; save(); render(); }, 900);
  setTimeout(() => {
    const p = ensurePairing();
    p.step = 'linked'; p.progress = 100;
    state.wearable.linked = true;
    state.wearable.connection = 'connected';
    state.wearable.lastSync = 'Ahora';
    state.sqliteReady = true;
    state.notifications.unshift({ id: Date.now(), title: 'Wearable vinculado por código', body: `${state.wearable.model} quedó asociado al teléfono Android.`, type: 'device', read: false, route: '/device' });
    save();
    toast('Wearable vinculado correctamente');
    render();
  }, 1350);
}
function continueAfterPairing() { if (!state.wearable.linked) return toast('Primero valida el código del reloj'); go('/wearable-permissions'); }
function toggleWearableSensor(key) { state.wearable.sensors[key] = !state.wearable.sensors[key]; save(); render(); }
function completeCalibration() { state.wearable.calibration = 100; save(); toast('Calibración completada'); go('/contacts-setup'); }
function finishOnboarding() { state.onboardingComplete = true; state.loggedIn = true; save(); go('/home'); }
function syncNow() { state.lastSync = 'Ahora'; state.wearable.lastSync = 'Ahora'; state.sqliteReady = true; state.notifications.unshift({ id: Date.now(), title: 'Sincronización completa', body: 'Nube, móvil, rutas y chat interno actualizados.', type: 'sync', read: false, route: '/sync' }); save(); toast('Sincronización completada'); render(); }
function toggleNetwork() { state.networkOnline = !state.networkOnline; save(); toast(state.networkOnline ? 'Internet restaurado' : 'Modo offline activado'); render(); }
function toggleWearableConnection() { state.wearable.connection = state.wearable.connection === 'connected' ? 'disconnected' : 'connected'; save(); render(); }
function unlinkWearable() { state.wearable.linked = false; state.wearable.connection = 'disconnected'; save(); toast('Wearable desvinculado'); render(); }
function newContactFlow() { if (activeContacts().length >= plan().contactsLimit) go('/plan-limit'); else go('/contact-form'); }
function saveNewContact() {
  if (activeContacts().length >= plan().contactsLimit) return go('/plan-limit');
  const identity = document.getElementById('contactIdentity')?.value?.trim() || '';
  const found = findDirectoryUser(identity);
  const name = document.getElementById('contactName').value.trim() || found?.name || 'Nuevo contacto';
  const username = found?.username || normalizeUsername(identity || name);
  const appUserId = found?.appUserId || generateUserId(username);
  const contact = {
    id: Date.now(), name, username, appUserId,
    relation: document.getElementById('contactRelation').value || found?.relation || 'Familiar',
    phone: document.getElementById('contactPhone').value || '+52 000 000 0000',
    email: username,
    priority: state.contacts.some(c => c.priority === 'Principal') ? 'Secundario' : 'Principal',
    status: 'Activo', channel: 'Chat interno',
    monitorId: null, notes: `Agregado desde prototipo móvil mediante ${identity ? 'usuario/ID' : 'captura manual'}.`
  };
  state.contacts.push(contact);
  state.sqliteReady = true;
  state.notifications.unshift({ id: Date.now()+1, title: 'Contacto agregado', body: `${name} fue agregado a la red interna como @${username}.`, type: 'contact', read: false, route: '/contacts' });
  save(); toast('Contacto guardado'); go(state.onboardingComplete ? '/contacts' : '/contacts-setup');
}
function makePrincipal(id) { state.contacts = state.contacts.map(c => ({ ...c, priority: c.id === id ? 'Principal' : 'Secundario' })); save(); toast('Contacto principal actualizado'); render(); }
function createMonitorFromContact(id) { const c = state.contacts.find(x => x.id === id); if (!c) return; const m = { id: Date.now(), contactId: c.id, name: c.name, username: c.username, appUserId: c.appUserId, phone: c.phone, email: c.email, status: 'Pendiente', token: `IX-MON-${Math.random().toString(36).slice(2,6).toUpperCase()}`, invitedAt: `${today()} ${nowTime()}`, acceptedAt: 'Pendiente', expiresAt: '7 días' }; state.monitors.push(m); c.monitorId = m.id; addInternalMessage('Sistema Impact.X', c.name, 'Solicitud familiar enviada', `${state.user.name} te invitó a su red familiar dentro de Impact.X.`, 'invite'); save(); toast('Solicitud interna generada'); go(`/monitor/${m.id}`); }
function deleteContact(id) { state.contacts = state.contacts.filter(c => c.id !== id); save(); toast('Contacto eliminado'); go('/contacts'); }
function saveNewMonitor() { const identity = document.getElementById('monitorIdentity')?.value?.trim() || ''; const found = findDirectoryUser(identity); const name = document.getElementById('monitorName').value.trim() || found?.name || 'Monitor invitado'; const username = found?.username || normalizeUsername(identity || name); const m = { id: Date.now(), contactId: null, name, username, appUserId: found?.appUserId || generateUserId(username), phone: document.getElementById('monitorPhone').value || '+52 000 000 0000', email: username, status: 'Pendiente', token: `IX-MON-${Math.random().toString(36).slice(2,6).toUpperCase()}`, invitedAt: `${today()} ${nowTime()}`, acceptedAt: 'Pendiente', expiresAt: '7 días' }; state.monitors.push(m); addInternalMessage('Sistema Impact.X', name, 'Solicitud familiar enviada', `${state.user.name} te invitó a su red familiar dentro de Impact.X.`, 'invite'); save(); toast('Invitación interna lista'); go(`/monitor/${m.id}`); }
function setMonitorStatus(id, status) { const m = state.monitors.find(x => x.id === id); if (!m) return; m.status = status; if (status === 'Activo') m.acceptedAt = `${today()} ${nowTime()}`; if (status === 'Revocado') m.expiresAt = 'Revocado'; save(); toast(`Monitor actualizado: ${status}`); render(); }
function shareMonitor(id) { const m = state.monitors.find(x => x.id === id); if (m) { m.status = 'Pendiente'; addInternalMessage('Sistema Impact.X', m.name, 'Solicitud familiar enviada', `Tienes una solicitud para unirte a la red familiar de ${state.user.name}. Código ${m.token}.`, 'invite'); } save(); toast('Solicitud enviada dentro de Impact.X'); render(); }
function payDemo() { toast('Pago exitoso'); state.notifications.unshift({ id: Date.now(), title: 'Pago confirmado', body: `Tu plan ${plan().name} fue renovado.`, type: 'plan', read: false, route: '/subscription' }); save(); go('/subscription'); }
function createDemoIncident() { const i = { id: Date.now(), type: 'Evento simulado', severity: 'Media', status: 'Simulación', date: today(), time: nowTime(), location: 'Ruta simulada, Tula', gps: '20.0531, -99.3432', gforce: '5.2G', decibels: plan().telemetry ? '96dB' : 'N/A', heart: plan().telemetry ? '112 bpm' : 'N/A', channel: 'Chat interno', response: '00:05', note: 'Incidente creado manualmente para probar historial.' }; state.incidents.unshift(i); save(); toast('Incidente creado'); go(`/incident/${i.id}`); }
function markNotification(id) { const n = state.notifications.find(x => x.id === id); if (n) { n.read = true; save(); go(n.route || '/notifications'); } }
function markAllNotifications() { state.notifications.forEach(n => n.read = true); save(); render(); }
function simulateBump() { go('/sos-countdown'); }
function criticalImpact() { if (plan().bypass) { toast('Bypass crítico Premium activado'); sendSos(); } else { toast('Tu plan requiere temporizador de confirmación'); go('/sos-countdown'); } }
function startManualSos() { sendSos(true); }
function startCountdown() {
  let value = 20;
  const el = () => document.getElementById('countVal');
  countdownTimer = setInterval(() => {
    value -= 1;
    if (el()) el().textContent = value;
    const ring = document.querySelector('.countdown-ring');
    if (ring) ring.style.setProperty('--p', Math.max(0, (value / 20) * 100));
    if (value <= 0) { clearCountdown(); sendSos(); }
  }, 1000);
}
function makeAlertObject(manual) { return { id: Date.now(), type: manual ? 'SOS manual' : 'Posible choque', severity: manual ? 'Alta' : 'Crítica', date: today(), time: nowTime(), location: state.trip?.active ? `Ruta activa: ${state.trip.routeName}` : 'Av. Universidad, Tula de Allende', gps: '20.0531, -99.3432', gforce: manual ? 'Manual' : '10.8G', decibels: plan().telemetry ? '132dB' : 'N/A', heart: plan().telemetry ? '142 bpm' : 'N/A', channel: state.networkOnline ? 'Chat interno' : 'Chat interno pendiente', response: 'En curso', note: state.trip?.active ? 'Alerta SOS activa durante viaje iniciado desde el wearable.' : 'Alerta SOS activa.' }; }
function sendSos(manual = false) { state.currentAlert = makeAlertObject(manual); save(); go('/sending-alert'); }
function finishSending(offline = false) { if (offline) state.networkOnline = false; if (!state.currentAlert) state.currentAlert = makeAlertObject(false); state.currentAlert.channel = state.networkOnline ? 'Chat interno' : 'Chat interno pendiente'; state.notifications.unshift({ id: Date.now(), title: 'SOS enviado', body: `Alerta publicada en ${state.currentAlert.channel}.`, type: 'alert', read: false, route: '/active-alert' }); save(); go('/active-alert'); }
function saveFalseAlarm() { const i = { id: Date.now(), type: 'Posible impacto', severity: 'Baja', status: 'Cancelado por usuario', date: today(), time: nowTime(), location: 'Ubicación actual', gps: '20.0531, -99.3432', gforce: '4.1G', decibels: plan().telemetry ? '88dB' : 'N/A', heart: plan().telemetry ? '98 bpm' : 'N/A', channel: 'No enviado', response: '00:07', note: document.getElementById('falseNote')?.value || 'Falsa alarma.' }; state.incidents.unshift(i); save(); toast('Falsa alarma registrada'); go(`/incident/${i.id}`); }
function closeIncident() { const alert = state.currentAlert || makeAlertObject(false); const i = { ...alert, id: Date.now(), status: 'Atendido', response: '03:12', note: document.getElementById('closeNote')?.value || 'Alerta atendida.' }; state.incidents.unshift(i); state.currentAlert = null; save(); toast('Incidente cerrado y guardado'); go(`/incident/${i.id}`); }
function acceptMonitorInvite() { state.appMode = 'monitor'; save(); toast('Invitación aceptada'); go('/monitor-home'); }

function normalizeUsername(value) {
  const clean = String(value || 'usuario_demo').toLowerCase().trim().replace(/^@/, '').replace(/[^a-z0-9_]/g, '_').replace(/_+/g, '_').replace(/^_|_$/g, '');
  return clean || 'usuario_demo';
}
function generateUserId(username) {
  const base = normalizeUsername(username).replace(/_/g,'').toUpperCase().slice(0,3) || 'USR';
  return `IX-${base}-${Math.floor(1000 + Math.random()*9000)}`;
}
function findDirectoryUser(identity) {
  const id = String(identity || '').trim().replace(/^@/, '').toLowerCase();
  if (!id) return null;
  return state.appDirectory.find(u => u.username.toLowerCase() === id || u.appUserId.toLowerCase() === id) || null;
}
function prefillInvite(username) {
  const input = document.getElementById('inviteIdentity');
  if (input) input.value = username;
  toast(`Usuario ${username} seleccionado`);
}
function fillRandomDirectoryUser() {
  const u = state.appDirectory[Math.floor(Math.random() * state.appDirectory.length)];
  prefillInvite(u.username);
}
function ensureContactFromUser(user, relation = 'Familiar') {
  let contact = state.contacts.find(c => c.username === user.username || c.appUserId === user.appUserId);
  if (contact) return contact;
  contact = {
    id: Date.now() + Math.floor(Math.random()*1000),
    name: user.name,
    username: user.username,
    appUserId: user.appUserId,
    relation: user.relation || relation,
    phone: '+52 000 000 0000',
    email: user.username,
    priority: state.contacts.some(c => c.priority === 'Principal') ? 'Secundario' : 'Principal',
    status: activeContacts().length < plan().contactsLimit ? 'Activo' : 'Suspendido por límite',
    channel: 'Chat interno',
    monitorId: null,
    notes: 'Agregado por solicitud interna usando usuario/ID Impact.X.'
  };
  state.contacts.push(contact);
  return contact;
}
function sendUserInvite() {
  const identity = document.getElementById('inviteIdentity')?.value?.trim() || '';
  const custom = document.getElementById('inviteMsg')?.value?.trim() || 'Quiero agregarte a mi red familiar de Impact.X.';
  const found = findDirectoryUser(identity) || { username: normalizeUsername(identity || 'nuevo_usuario'), appUserId: generateUserId(identity || 'nuevo_usuario'), name: identity ? identity.replace(/^@/,'') : 'Usuario invitado', relation: 'Familiar' };
  const contact = ensureContactFromUser(found);
  let monitor = state.monitors.find(m => m.username === found.username || m.appUserId === found.appUserId);
  if (!monitor) {
    monitor = { id: Date.now()+7, contactId: contact.id, name: found.name, username: found.username, appUserId: found.appUserId, phone: contact.phone, email: found.username, status: 'Pendiente', token: `IX-MON-${Math.random().toString(36).slice(2,6).toUpperCase()}`, invitedAt: `${today()} ${nowTime()}`, acceptedAt: 'Pendiente', expiresAt: '7 días' };
    state.monitors.push(monitor);
    contact.monitorId = monitor.id;
  } else {
    monitor.status = 'Pendiente';
    monitor.invitedAt = `${today()} ${nowTime()}`;
  }
  addInternalMessage('Sistema Impact.X', found.name, 'Solicitud familiar enviada', custom, 'invite');
  addInternalMessage(found.name, state.user.name, 'Solicitud recibida', 'Recibí tu solicitud dentro de Impact.X. La revisaré para aceptar la red familiar.', 'reply');
  state.notifications.unshift({ id: Date.now()+12, title: 'Solicitud interna enviada', body: `Invitación enviada a @${found.username}.`, type: 'invite', read: false, route: '/chat' });
  save();
  toast(`Solicitud enviada a @${found.username}`);
  go('/chat');
}
function activeReplyNames() {
  const active = state.monitors.filter(m => m.status === 'Activo');
  if (active.length) return active.map(m => m.name).slice(0,2);
  return activeContacts().map(c => c.name).slice(0,2);
}
function simulateRepliesFor(title, body) {
  const names = activeReplyNames();
  const route = selectedRoute();
  const replies = [
    'Recibido, estaré pendiente desde Impact.X.',
    route ? `Gracias por avisar. Ya vi la ruta ${route.label}.` : 'Gracias por avisar, te leo por aquí.',
    'Ok, cualquier cosa reviso la alerta interna.'
  ];
  names.forEach((name, idx) => {
    addInternalMessage(name, state.user.name, 'Respuesta recibida', replies[idx % replies.length], 'reply');
  });
}
function simulateIncomingReply() {
  const names = activeReplyNames();
  const name = names[0] || 'Monitor demo';
  addInternalMessage(name, state.user.name, 'Mensaje recibido', 'Te confirmo por Impact.X. Estoy pendiente de tu ruta y alertas.', 'reply');
  save();
  toast('Respuesta simulada recibida');
  render();
}
function sendCustomInternalMessage() {
  const body = document.getElementById('customChatMsg')?.value?.trim();
  const recipient = document.getElementById('chatRecipient')?.value || 'Red familiar';
  if (!body) return toast('Escribe un mensaje antes de enviarlo');
  state.chatDraft.lastCustomBody = body;
  state.chatDraft.lastRecipient = recipient;
  addInternalMessage(state.user.name, recipient, 'Mensaje personalizado', body, 'me');
  simulateRepliesFor('Mensaje personalizado', body);
  state.notifications.unshift({ id: Date.now()+3, title: 'Mensaje interno enviado', body: `Mensaje enviado a ${recipient}.`, type: 'chat', read: false, route: '/chat' });
  save(); toast('Mensaje interno enviado'); render();
}
function sendCustomTemplateMessage() {
  const body = document.getElementById('templateCustomMsg')?.value?.trim();
  const recipient = document.getElementById('templateRecipient')?.value || 'Red familiar';
  if (!body) return toast('Escribe un mensaje antes de enviarlo');
  addInternalMessage(state.user.name, recipient, 'Mensaje personalizado', body, 'me');
  simulateRepliesFor('Mensaje personalizado', body);
  save(); toast('Mensaje personalizado enviado'); go('/chat');
}
function sendRouteCustomMessage() {
  const selected = selectedRoute();
  const body = document.getElementById('routeCustomMsg')?.value?.trim() || `Hoy tomaré ${selected ? selected.label : 'mi ruta de hoy'}.`;
  addInternalMessage(state.user.name, 'Red familiar', 'Ruta personalizada', body, 'route');
  state.routeDraft.todayRouteShared = true;
  state.routeDraft.lastSharedAt = `${today()} ${nowTime()}`;
  simulateRepliesFor('Ruta personalizada', body);
  save(); toast('Ruta enviada con mensaje personalizado'); go('/chat');
}
function sendDirectContactMessage(id) {
  const c = state.contacts.find(x => x.id === id);
  if (!c) return;
  addInternalMessage(state.user.name, c.name, 'Mensaje directo', `Hola ${c.name}, te agregué a mi red familiar de Impact.X.`, 'me');
  addInternalMessage(c.name, state.user.name, 'Respuesta recibida', 'Recibido, ya me aparece la solicitud dentro de Impact.X.', 'reply');
  save(); toast('Mensaje directo simulado'); go('/chat');
}

function addInternalMessage(from, to, title, body, type = 'chat') {
  state.internalMessages.unshift({ id: Date.now() + Math.floor(Math.random()*999), from, to, title, body, time: nowTime(), type });
}

function addFrequentRoute() {
  const name = document.getElementById('routeName')?.value?.trim() || 'Nueva ruta frecuente';
  const label = document.getElementById('routeLabel')?.value?.trim() || name;
  const origin = document.getElementById('routeOrigin')?.value?.trim() || 'Origen';
  const destination = document.getElementById('routeDestination')?.value?.trim() || 'Destino';
  const note = document.getElementById('routeNote')?.value?.trim() || 'Ruta frecuente.';
  const route = { id: Date.now(), label, name, origin, destination, note, lastUsed: 'Hoy' };
  state.frequentRoutes.unshift(route);
  state.routeDraft.selectedRouteId = route.id;
  state.routeDraft.todayRouteShared = false;
  save();
  toast('Ruta frecuente guardada');
  render();
}

function selectRouteToday(id, notify = false) {
  const r = state.frequentRoutes.find(x => x.id === id);
  if (!r) return;
  state.routeDraft.selectedRouteId = id;
  state.trip.routeName = r.label;
  r.lastUsed = 'Hoy';
  state.routeDraft.todayRouteShared = false;
  if (notify) sendTemplateToMonitors(801);
  save();
  toast(notify ? 'Ruta avisada por chat interno' : 'Ruta seleccionada para hoy');
  render();
}

function sendTemplateToMonitors(templateId) {
  const t = state.messageTemplates.find(x => x.id === templateId);
  const r = selectedRoute();
  if (!t) return;
  const body = t.body.replace('{ruta}', r ? r.label : 'ruta seleccionada');
  addInternalMessage(state.user.name, 'Red familiar', t.title, body, t.title.toLowerCase().includes('ruta') ? 'route' : 'template');
  simulateRepliesFor(t.title, body);
  state.routeDraft.todayRouteShared = true;
  state.routeDraft.lastSharedAt = `${today()} ${nowTime()}`;
  state.notifications.unshift({ id: Date.now()+2, title: 'Mensaje interno enviado', body: `${t.title}: ${r ? r.label : 'ruta seleccionada'}`, type: 'chat', read: false, route: '/chat' });
  save();
  toast('Mensaje interno enviado y respuestas simuladas recibidas');
  render();
}

function receiveWearableTripStart() {
  const selected = selectedRoute();
  state.trip = {
    ...state.trip,
    active: true,
    paused: false,
    startedAt: Date.now(),
    startedLabel: nowTime(),
    routeName: selected ? selected.label : state.trip.routeName,
    purpose: 'Inicio desde wearable',
    gpsConsent: true,
    backgroundConsent: true,
    shareWithMonitors: true,
    permissionToken: `IX-WEAR-${Math.random().toString(36).slice(2,8).toUpperCase()}`,
    autoDetectEnabled: true,
    lastCheckpoint: `${today()} ${nowTime()} · viaje iniciado desde wearable`
  };
  if (selected && !state.routeDraft.todayRouteShared) sendTemplateToMonitors(801);
  state.notifications.unshift({ id: Date.now(), title: 'Viaje iniciado desde wearable', body: `Monitoreo visible para ${state.trip.routeName}.`, type: 'trip', read: false, route: '/trip-active' });
  save();
  toast('Evento recibido desde wearable');
  go('/trip-active');
}

function startTrip() {
  // Alias conservado para compatibilidad del prototipo anterior: ahora simula recepción desde wearable.
  receiveWearableTripStart();
}
function pauseTrip() {
  if (!state.trip.active) return;
  state.trip.paused = !state.trip.paused;
  state.trip.lastCheckpoint = `${today()} ${nowTime()} · ${state.trip.paused ? 'viaje pausado' : 'viaje reanudado'}`;
  save();
  toast(state.trip.paused ? 'Viaje pausado' : 'Viaje reanudado');
  render();
}
function finishTrip() {
  if (!state.trip.active) return go('/home');
  const live = tripTelemetry();
  const summary = {
    id: Date.now(),
    routeName: state.trip.routeName,
    purpose: state.trip.purpose,
    startedAt: state.trip.startedLabel,
    endedAt: `${today()} ${nowTime()}`,
    duration: tripElapsedLabel(),
    distance: live.distance,
    avgSpeed: Math.max(22, Math.round(live.speed * .72)),
    risk: live.risk,
    token: state.trip.permissionToken
  };
  state.tripHistory.unshift(summary);
  state.trip.active = false;
  state.trip.paused = false;
  state.trip.lastEndedAt = summary.endedAt;
  state.trip.lastCheckpoint = `${summary.endedAt} · viaje finalizado`;
  state.notifications.unshift({ id: Date.now()+1, title: 'Viaje finalizado', body: `${summary.distance} km registrados desde wearable.`, type: 'trip', read: false, route: '/trip-summary' });
  save();
  toast('Viaje finalizado y resumen guardado');
  go('/trip-summary');
}
function tripBump() {
  toast('Bache fuerte detectado: se abre confirmación para evitar falsa alarma');
  go('/sos-countdown');
}

window.addEventListener('hashchange', render);
window.addEventListener('load', () => { if (!location.hash) location.hash = '/splash'; render(); });
