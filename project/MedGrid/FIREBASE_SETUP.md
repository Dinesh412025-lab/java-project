# Firebase Authentication setup

MedGrid serves its Firebase web configuration from the /api/firebase-config endpoint. The values below are public Firebase web-app identifiers, not passwords or service-account credentials.

## 1. Create/configure Firebase

1. Open the Firebase Console at https://console.firebase.google.com/ and create or select a project.
2. Add a Web app and copy its configuration values.
3. In Authentication → Sign-in method, enable Google and Email/Password.
4. In Authentication → Settings → Authorized domains, add the domain where MedGrid runs (and localhost for local development).

## 2. Configure the MedGrid server

Set these environment variables before starting the Java server:

- FIREBASE_PROJECT_ID — Firebase project ID
- FIREBASE_WEB_API_KEY — Web app API key
- FIREBASE_APP_ID — Web app ID
- FIREBASE_AUTH_DOMAIN — optional; defaults to <project-id>.firebaseapp.com

Example:

    export FIREBASE_PROJECT_ID=your-project-id
    export FIREBASE_WEB_API_KEY=your-web-api-key
    export FIREBASE_APP_ID=1:1234567890:web:abcdef123456
    export FIREBASE_AUTH_DOMAIN=your-project-id.firebaseapp.com
    java --add-modules jdk.httpserver -cp out com.medgrid.Main

The browser signs users in through Firebase using Google or email/password. Firebase handles password storage; MedGrid does not store passwords in users.csv.
