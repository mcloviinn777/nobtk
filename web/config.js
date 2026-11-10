// NOBTK settings. Edit this file to connect the app to your Firebase project.
window.NOBTK_CONFIG = {
  // Paste your Firebase web config here (Project settings > Your apps > Web app).
  // While this stays null, the app runs offline on one device only.
  firebase: {
    apiKey: "AIzaSyBkfsgowk73oq-J3hO89-OuG_a-P0GB7pk",
    authDomain: "nobtk-40d29.firebaseapp.com",
    projectId: "nobtk-40d29",
    storageBucket: "nobtk-40d29.firebasestorage.app",
    messagingSenderId: "653012174577",
    appId: "1:653012174577:web:64689069d2feb4965c4b50"
  },

  // One id per barbershop. The client link and the barber dashboard use the same id.
  shopId: "barber-mohammedia",

  // Code the barber types to open the dashboard and the TV screen.
  // Pilot-level protection only: anyone who reads the site's code can see it.
  barberPin: "2580"
};
