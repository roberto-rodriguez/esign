const state = { sessionId: null, lang: 'en', docs: { esign: null, others: [] }, viewed: { esign: false, others: false } };

const text = {
  en: {
    title: 'E-Sign Consent',
    welcome: 'Please review and accept the agreements',
    instructions: 'You must open E-Sign first, then open the other agreements.',
    esign: 'By checking this box, I confirm that I have read and agree to the Electronic Communications “ESIGN” Agreement.',
    others: 'By checking this box, I confirm that I have read and agree to the Cardholder Agreement, Fee Schedule and Privacy Policy.',
    submitOk: 'Signature completed successfully.'
  },
  es: {
    title: 'Consentimiento de Firma Electrónica',
    welcome: 'Revise y acepte los acuerdos',
    instructions: 'Debe abrir primero E-Sign y luego los otros acuerdos.',
    esign: 'Al marcar esta casilla, confirmo que he leído y acepto el Acuerdo de Comunicaciones Electrónicas “ESIGN”.',
    others: 'Al marcar esta casilla, confirmo que he leído y acepto el Acuerdo del Tarjetahabiente, Tabla de Cargos y Política de Privacidad.',
    submitOk: 'Firma completada correctamente.'
  }
};

function renderText() {
  const t = text[state.lang];
  document.getElementById('welcome').textContent = t.welcome;
  document.getElementById('instructions').textContent = t.instructions;
  document.getElementById('text-esign').textContent = t.esign;
  document.getElementById('text-others').textContent = t.others;
}

async function initSession() {
  const p = new URLSearchParams(location.search);
  state.lang = (p.get('lang') || 'en').toLowerCase() === 'es' ? 'es' : 'en';

  const payload = {
    fullName: p.get('fullName') || 'Mock User',
    ipAddress: p.get('ipAddress') || '',
    phoneNumber: p.get('phoneNumber') || '0000000000',
    callbackUrl: p.get('callbackUrl') || 'http://localhost:8080/mock-callback',
    defaultLanguage: state.lang
  };

  const initRes = await fetch('/api/esign/init', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  });
  const initBody = await initRes.json();
  state.sessionId = initBody.sessionId;

  const cfgRes = await fetch(`/api/esign/${state.sessionId}/config`);
  const cfg = await cfgRes.json();
  state.lang = cfg.language;
  state.docs.esign = cfg.esignDocumentUrl;
  state.docs.others = cfg.otherDocumentUrls || [];
}

function bindEvents() {
  document.getElementById('open-esign').onclick = () => openEsign();
  document.getElementById('open-others').onclick = () => openOthers();
  document.getElementById('check-esign').onchange = onEsignCheckChanged;
  document.getElementById('check-others').onchange = updateSubmit;
  document.getElementById('submit').onclick = submitSignature;
}

function showDocuments(urls) {
  const viewer = document.getElementById('pdf-viewer');
  viewer.innerHTML = '';
  urls.forEach(url => {
    const frame = document.createElement('iframe');
    frame.src = url;
    frame.title = 'PDF viewer';
    viewer.appendChild(frame);
  });
}

async function openEsign() {
  showDocuments([state.docs.esign]);
  state.viewed.esign = true;
  await fetch(`/api/esign/${state.sessionId}/viewed/esign`, { method: 'POST' });
  document.getElementById('check-esign').disabled = false;
}

async function openOthers() {
  if (!state.viewed.esign) return;
  showDocuments(state.docs.others);
  state.viewed.others = true;
  await fetch(`/api/esign/${state.sessionId}/viewed/others`, { method: 'POST' });
  document.getElementById('check-others').disabled = false;
}


function onEsignCheckChanged() {
  const esignChecked = document.getElementById('check-esign').checked;
  document.getElementById('open-others').disabled = !esignChecked;
  if (!esignChecked) {
    document.getElementById('check-others').checked = false;
    document.getElementById('check-others').disabled = true;
    state.viewed.others = false;
  }
  updateSubmit();
}

function updateSubmit() {
  const canSubmit = document.getElementById('check-esign').checked && document.getElementById('check-others').checked;
  document.getElementById('submit').disabled = !canSubmit;
}

async function submitSignature() {
  const payload = {
    sessionId: state.sessionId,
    esignAccepted: document.getElementById('check-esign').checked,
    otherAgreementsAccepted: document.getElementById('check-others').checked
  };
  const res = await fetch('/api/esign/submit', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  });
  if (res.ok) document.getElementById('message').textContent = text[state.lang].submitOk;
}

(async function bootstrap() {
  bindEvents();
  await initSession();
  renderText();
})();
