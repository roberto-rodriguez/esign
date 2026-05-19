const state = { sessionId: null, lang: 'en', viewed: { esign: false, others: false } };

const text = {
  en: {
    title: 'E-Sign Consent',
    welcome: 'Please review and accept the agreements',
    instructions: 'You must open E-Sign first, then open the other agreements. Checkboxes are enabled after viewing.',
    esign: 'By checking this box, I confirm that I have read and agree to the Electronic Communications “ESIGN” Agreement.',
    others: 'By checking this box, I confirm that I have read and agree to the Cardholder Agreement, Fee Schedule and Privacy Policy.',
    submitOk: 'Signature completed successfully.'
  },
  es: {
    title: 'Consentimiento de Firma Electrónica',
    welcome: 'Revise y acepte los acuerdos',
    instructions: 'Debe abrir primero E-Sign y luego los otros acuerdos. Las casillas se habilitan después de revisar.',
    esign: 'Al marcar esta casilla, confirmo que he leído y acepto el Acuerdo de Comunicaciones Electrónicas “ESIGN”.',
    others: 'Al marcar esta casilla, confirmo que he leído y acepto el Acuerdo del Tarjetahabiente, Tabla de Cargos y Política de Privacidad.',
    submitOk: 'Firma completada correctamente.'
  }
};

function renderText() {
  const t = text[state.lang];
  document.getElementById('title').textContent = t.title;
  document.getElementById('welcome').textContent = t.welcome;
  document.getElementById('instructions').textContent = t.instructions;
  document.getElementById('text-esign').textContent = t.esign;
  document.getElementById('text-others').textContent = t.others;
}

async function initSession() {
  const params = new URLSearchParams(window.location.search);
  const payload = {
    fullName: params.get('fullName') || 'Mock User',
    ipAddress: params.get('ipAddress') || '',
    phoneNumber: params.get('phoneNumber') || '0000000000',
    callbackUrl: params.get('callbackUrl') || 'http://localhost:8080/mock-callback'
  };

  const res = await fetch('/api/esign/init', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  });
  const body = await res.json();
  state.sessionId = body.sessionId;
}

function bindEvents() {
  document.getElementById('lang-en').onclick = () => { state.lang = 'en'; toggleLang(); };
  document.getElementById('lang-es').onclick = () => { state.lang = 'es'; toggleLang(); };

  document.getElementById('open-esign').onclick = () => openDoc('esign', '/api/esign/documents/esign-requirements.pdf');
  document.getElementById('open-others').onclick = () => openDoc('others', '/api/esign/documents/other-agreements.pdf');

  document.getElementById('check-esign').onchange = updateSubmit;
  document.getElementById('check-others').onchange = updateSubmit;

  document.getElementById('submit').onclick = submit;
}

function toggleLang() {
  document.getElementById('lang-en').classList.toggle('active', state.lang === 'en');
  document.getElementById('lang-es').classList.toggle('active', state.lang === 'es');
  renderText();
}

async function openDoc(type, url) {
  if (type === 'others' && !state.viewed.esign) return;
  document.getElementById('pdf-viewer').src = url;
  state.viewed[type] = true;
  await fetch(`/api/esign/${state.sessionId}/viewed/${type}`, { method: 'POST' });
  if (state.viewed.esign) document.getElementById('check-esign').disabled = false;
  if (state.viewed.esign && state.viewed.others) document.getElementById('check-others').disabled = false;
}

function updateSubmit() {
  const can = document.getElementById('check-esign').checked && document.getElementById('check-others').checked;
  document.getElementById('submit').disabled = !can;
}

async function submit() {
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
  renderText();
  bindEvents();
  await initSession();
})();
