# TactiCode (jeu HTML + APK Android)

Jeu tactique multijoueur (2 à 8 joueurs) avec **code de salle** et **chat vocal (micro)**.
Le jeu est dans `www/index.html`. GitHub Actions le transforme en APK avec Capacitor.

## Faire l APK avec GitHub

1. Crée un dépôt GitHub vide.
2. Décompresse ce ZIP et envoie TOUT à la racine du dépôt (avec le dossier caché `.github`).
3. Onglet **Actions** > **Build Android APK** (se lance seul, ou *Run workflow*). 5 à 10 minutes.
4. Télécharge **TactiCode-APK** (section *Artifacts*), décompresse, installe `app-debug.apk`.
5. Au premier lancement, accepte la permission **Micro**.

## Tester sans APK

Ouvre `www/index.html` dans Chrome (ou sur un petit serveur local). Le mode Practice marche tout de suite.

## Comment jouer à plusieurs

- Joueur 1 : **CREATE ROOM**, il reçoit un code (ex. K7M4QX) et le partage.
- Joueurs 2 à 8 : tapent le code et **JOIN**.
- Le host lance **START MATCH**. Dernier survivant gagne.
- Contrôles tactile : stick gauche = bouger, stick droit = viser et tirer. PC : WASD + souris.

## Réseau et data (important)

- Le jeu et la voix passent en **WebRTC pair-à-pair** (PeerJS). Un petit service de rencontre PeerJS
  est utilisé uniquement pour établir la connexion (quelques Ko). **Il faut donc un accès internet
  au moment de rejoindre.** Le mode Practice marche 100 % hors ligne.
- Le chat vocal consomme plus que le jeu lui-même. Sur un même Wi-Fi / hotspot, le trafic reste
  entre les téléphones, mais à tester sur tes appareils (comportement variable selon les réseaux).
- Pour du vrai 100 % hors ligne, la prochaine étape est le plugin Nearby Connections / Wi-Fi Direct.

## Personnaliser

Icône de l app : ajoute `@capacitor/assets`. Verrouiller l orientation : modifie le manifest dans le workflow.
