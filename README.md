# Zone Rush (Android, 100% hors-ligne)

Battle royale 2D original, theme Sahara. Zone qui retrecit, loot, bots, boutique,
emotes/danses, chat vocal, multijoueur local par code (Bluetooth / Wi-Fi direct, SANS internet).

## Compiler l'APK avec GitHub
1. Cree un depot GitHub, envoie TOUT le contenu de ce dossier (dont `.github/`).
2. Onglet **Actions** > "Build APK" (se lance au push, ou "Run workflow").
3. Telecharge l'artefact **ZoneRush-APK** > `app-debug.apk`, installe sur Android 8+.

## Ouvrir dans Android Studio
File > Open > dossier ZoneRush (le wrapper Gradle est genere automatiquement).

## Multijoueur sans internet
- Telephones proches (meme piece, ~30 m), Bluetooth + Wi-Fi + localisation actives.
- Hote: "Creer un salon" > donne le code a 4 chiffres. Joueurs: "Rejoindre avec un code".
- Necessite Google Play Services (Nearby Connections).
- Maintiens 🎤 pour parler. 😀 pour danser; meme danse a plusieurs = bonus de pieces.
