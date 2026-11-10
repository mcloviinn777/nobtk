# NOBTK · نوبتك

**Ton tour, sans attendre.** Application de file d'attente pour les barbiers au Maroc : les clients prennent leur ticket depuis leur téléphone, suivent leur place en direct et sont prévenus avant leur tour. Le barbier appuie sur « Terminé » et le client suivant passe automatiquement.

## Les liens

| Page | Lien | Pour qui |
|---|---|---|
| Application client | https://mcloviinn777.github.io/nobtk/ | Les clients (lien du QR code en vitrine) |
| Tableau du barbier | https://mcloviinn777.github.io/nobtk/#barber | Le barbier (code du salon demandé) |
| Écran TV du salon | https://mcloviinn777.github.io/nobtk/#screen | La télé du salon |
| Démo (client + barbier) | https://mcloviinn777.github.io/nobtk/#demo | Présentations |
| Formulaire d'inscription | https://mcloviinn777.github.io/nobtk/inscription.html | Les barbiers qui veulent NOBTK |
| Tableau de bord admin | https://mcloviinn777.github.io/nobtk/admin.html | Toi (connexion email) |
| APK Android du barbier | https://github.com/mcloviinn777/nobtk/releases/latest | Le téléphone du barbier |

## Comment c'est construit

- `web/` : le site (HTML/JS, sans installation). Publié automatiquement sur GitHub Pages à chaque modification.
- `web/config.js` : connexion Firebase, identifiant du salon (`shopId`) et code barbier (`barberPin`).
- `android/` : l'application Android du barbier, une fenêtre plein écran sur le tableau du barbier. L'APK est construit automatiquement par GitHub Actions et publié dans **Releases**.
- `firestore.rules` : les règles de sécurité de la base Firebase (copie de celles en ligne).

## Les données (Firebase Firestore)

- `shops/{shopId}` : réglages du salon (nom, pause, fauteuils, délai, blocage).
- `shops/{shopId}/tickets` : les tickets de la journée.
- `shops/{shopId}/clients` : les absences par numéro de téléphone.
- `signups` : les inscriptions envoyées par le formulaire (lisibles seulement par toi, connecté).

## Changer le code barbier ou ajouter un salon

Modifie `web/config.js` sur GitHub (bouton crayon), puis « Commit changes ». Le site se met à jour en 1 à 2 minutes.

## Limites de la version pilote

- Le tableau du barbier est protégé par un code dans l'appli, pas par un vrai compte. C'est suffisant pour un test, à renforcer avant de vendre à plusieurs salons.
- Les alertes s'affichent quand le client a la page NOBTK ouverte (ou installée sur l'écran d'accueil). Des notifications sur téléphone verrouillé demandent Firebase Cloud Messaging ou une appli Play Store.
