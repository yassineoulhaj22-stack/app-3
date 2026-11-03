# TactiCode (prototype Android)

Prototype Kotlin + Jetpack Compose du jeu tactique offline-first avec salles privées par code.

## Générer l APK avec GitHub (sans Android Studio)

1. Crée un dépôt vide sur GitHub (ex. `TactiCode`).
2. Décompresse ce ZIP, puis envoie TOUT le contenu à la racine du dépôt
   (y compris le dossier caché `.github`). Sur le site GitHub : *Add file > Upload files*,
   ou en ligne de commande :
   ```
   git init && git add . && git commit -m "init"
   git branch -M main
   git remote add origin https://github.com/TON_NOM/TactiCode.git
   git push -u origin main
   ```
3. Ouvre l onglet **Actions** : le workflow **Build APK** démarre tout seul
   (ou clique sur *Run workflow*). Compte 3 à 6 minutes.
4. Quand il est vert, ouvre le run et télécharge **TactiCode-debug-apk** en bas
   (section *Artifacts*). Décompresse-le pour obtenir `app-debug.apk`.
5. Copie l APK sur ton téléphone et installe-le
   (autorise « Installer des applications inconnues »).

## Ce que contient le prototype

- Accueil, saisie du code (6 cases + clavier sans caractères ambigus, bouton Coller)
- Création de salle (code généré, Copier, Partager)
- Lobby avec joueurs simulés, ping, ready check
- Maquette de HUD (minimap, zone, santé, munitions, ping, compteur de données)
- Astuce de démo : le code AAAAAA affiche l état d erreur « Room not found »

## Pas encore implémenté

Le réseau réel (P2P local, relais, chiffrement par code) : les joueurs du lobby sont
simulés dans `LobbyScreen`. Prochaine étape : ajouter les permissions réseau dans
`AndroidManifest.xml` et une couche de transport (UDP / Nearby Connections).

## Palette

Fond #0F172A, surface #1E293B, accent #10B981 (voir `MainActivity.kt`).
