import { initializeApp } from 'https://www.gstatic.com/firebasejs/11.6.1/firebase-app.js';
import { getAuth, GoogleAuthProvider, createUserWithEmailAndPassword, onAuthStateChanged, signInWithEmailAndPassword, signInWithPopup, signOut } from 'https://www.gstatic.com/firebasejs/11.6.1/firebase-auth.js';

let auth;
let mode = 'signin';
let pollingStarted = false;
const originalFetch = window.fetch.bind(window);

function errorText(error) {
    const messages = {
        'auth/invalid-credential': 'The email or password is incorrect.',
        'auth/email-already-in-use': 'An account already exists for this email.',
        'auth/weak-password': 'Use a password with at least 6 characters.',
        'auth/invalid-email': 'Enter a valid email address.',
        'auth/popup-closed-by-user': 'The Google sign-in window was closed.',
        'auth/popup-blocked': 'Allow pop-ups in your browser to continue with Google.',
        'auth/too-many-requests': 'Too many attempts. Please wait and try again.',
        'auth/operation-not-allowed': 'This sign-in method is not enabled in Firebase yet.',
        'auth/network-request-failed': 'Network error. Check your connection and try again.',
        'auth/unauthorized-domain': 'This app domain is not authorized in Firebase.'
    };
    return messages[error?.code] || 'Authentication failed. Please try again.';
}

function showError(message) {
    const el = document.getElementById('login-error');
    if (!el) return;
    el.textContent = message;
    el.style.display = 'block';
}

function clearError() {
    const el = document.getElementById('login-error');
    if (el) { el.textContent = ''; el.style.display = 'none'; }
}

function setupPrivacyFirstUi() {
    const card = document.querySelector('.login-card');
    const form = document.getElementById('login-form');
    const emailInput = document.getElementById('login-email') || document.getElementById('login-username');
    const password = document.getElementById('login-password');
    const loginButton = document.getElementById('login-btn');
    if (!card || !form || !emailInput || !password || !loginButton) return;
    if (form.dataset.firebaseAuthBound === 'true') return;
    form.dataset.firebaseAuthBound = 'true';

    emailInput.type = 'email';
    emailInput.id = 'login-email';
    emailInput.name = 'email';
    emailInput.placeholder = 'Email address';
    emailInput.autocomplete = 'email';
    password.autocomplete = 'current-password';
    password.minLength = 6;

    let google = document.getElementById('google-login-btn');
    if (!google) {
        google = document.createElement('button');
        google.type = 'button';
        google.id = 'google-login-btn';
        google.className = 'google-login-btn';
        form.parentNode.insertBefore(google, form);
    }
    google.textContent = 'G  Continue with Google';

    if (!form.parentNode.querySelector('.login-divider')) {
        const divider = document.createElement('div');
        divider.className = 'login-divider';
        divider.textContent = 'or use email';
        form.parentNode.insertBefore(divider, form);
    }

    let toggle = document.getElementById('auth-mode-toggle');
    if (!toggle) {
        toggle = document.createElement('button');
        toggle.type = 'button';
        toggle.id = 'auth-mode-toggle';
        toggle.className = 'auth-mode-toggle';
        form.parentNode.insertBefore(toggle, document.getElementById('login-error'));
    }
    toggle.textContent = 'Create a new account';

    if (!form.parentNode.querySelector('.privacy-note')) {
        const privacy = document.createElement('p');
        privacy.className = 'privacy-note';
        privacy.textContent = 'Your password is handled by Firebase Authentication and is never stored by MedGrid.';
        form.parentNode.insertBefore(privacy, document.getElementById('login-error'));
    }

    toggle.addEventListener('click', () => {
        mode = mode === 'signin' ? 'signup' : 'signin';
        loginButton.textContent = mode === 'signup' ? 'Create account' : 'Sign in with email';
        password.autocomplete = mode === 'signup' ? 'new-password' : 'current-password';
        toggle.textContent = mode === 'signup' ? 'Already have an account? Sign in' : 'Create a new account';
        clearError();
    });

    google.addEventListener('click', async () => {
        clearError();
        google.disabled = true;
        google.textContent = 'Opening Google...';
        try {
            if (!auth) { showError('Firebase Authentication is not configured on this server.'); return; }
            await signInWithPopup(auth, new GoogleAuthProvider());
        } catch (error) {
            showError(errorText(error));
        } finally {
            google.disabled = false;
            google.textContent = 'G  Continue with Google';
        }
    });

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        event.stopImmediatePropagation();
        clearError();
        loginButton.disabled = true;
        loginButton.textContent = mode === 'signup' ? 'Creating account...' : 'Signing in...';
        try {
            if (!auth) { showError('Firebase Authentication is not configured on this server.'); return; }
            const email = document.getElementById('login-email').value.trim();
            if (mode === 'signup') await createUserWithEmailAndPassword(auth, email, password.value);
            else await signInWithEmailAndPassword(auth, email, password.value);
        } catch (error) {
            showError(errorText(error));
        } finally {
            loginButton.disabled = false;
            loginButton.textContent = mode === 'signup' ? 'Create account' : 'Sign in with email';
        }
    }, true);

    const note = card.querySelector('p:not(#login-error):not(.privacy-note)');
    if (note) note.textContent = 'Sign in securely with Google or your email address.';
}

function setupUserControls() {
    const controls = document.querySelector('.controls');
    if (!controls || document.getElementById('logout-btn')) return;
    const wrapper = document.createElement('div');
    wrapper.className = 'user-controls';
    wrapper.innerHTML = '<span id="auth-user-email" class="auth-user-email"></span><button type="button" id="logout-btn" class="logout-btn">Sign out</button>';
    controls.appendChild(wrapper);
    document.getElementById('logout-btn').addEventListener('click', () => signOut(auth));
}

function startPolling() {
    if (pollingStarted) return;
    pollingStarted = true;
    setInterval(window.fetchDashboardData, 1000);
    setInterval(window.fetchAI2Data, 2000);
    window.fetchDashboardData();
    window.fetchHistoryData();
    window.fetchAI2Data();
    window.runTriagePrediction('chest pain, shortness of breath, sweating');
}

window.fetch = async (input, options = {}) => {
    const url = typeof input === 'string' ? input : input.url;
    if (url && url.startsWith('/api/') && !url.startsWith('/api/firebase-config') && auth?.currentUser) {
        const token = await auth.currentUser.getIdToken();
        const headers = new Headers(options.headers || (input instanceof Request ? input.headers : undefined));
        headers.set('Authorization', 'Bearer ' + token);
        options = { ...options, headers };
    }
    return originalFetch(input, options);
};

(async () => {
    setupPrivacyFirstUi();
    try {
        const response = await originalFetch('/api/firebase-config');
        if (!response.ok) throw new Error('Firebase is not configured on this server.');
        const config = await response.json();
        auth = getAuth(initializeApp(config));
        setupPrivacyFirstUi();
        onAuthStateChanged(auth, (user) => {
            const overlay = document.getElementById('login-overlay');
            const app = document.getElementById('app-container');
            if (user) {
                overlay.style.display = 'none';
                app.style.display = 'flex';
                setupUserControls();
                const email = document.getElementById('auth-user-email');
                if (email) email.textContent = user.email || 'Google account';
                startPolling();
            } else {
                overlay.style.display = 'flex';
                app.style.display = 'none';
            }
        });
    } catch (error) {
        showError(error.message || 'Unable to load Firebase Authentication.');
    }
})();
