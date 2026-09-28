# WhAlert

Application Android (Kotlin / Jetpack Compose / Material 3) pour **documenter et signaler
légitimement** des comptes WhatsApp suspects, en respectant strictement les mécanismes
officiels de WhatsApp. Package : `com.whalert.app`.

## Ce que l'application fait réellement
- Prépare un **dossier de signalement** : numéro (validation E.164 stricte via libphonenumber),
  catégorie (spam, arnaque, usurpation d'identité, harcèlement, contenu frauduleux,
  comportement malveillant, autre), description factuelle, date/heure, preuves (captures via SAF).
- Génère un **texte de signalement professionnel** (aperçu obligatoire, modifier/copier/annuler/envoyer).
- **Double confirmation explicite** avant toute transmission ; aucune transmission automatique.
- Redirige vers le **canal officiel WhatsApp** (formulaire support officiel ou signalement in-app
  documenté) — jamais d'API privée, jamais d'endpoint inventé.
- Suit le dossier : brouillon → préparé → soumis → confirmation technique → **décision de
  modération : Inconnue** (WhatsApp ne fournit pas de confirmation publique de suspension ;
  l'app l'affiche honnêtement).
- Historique **chiffré localement** (Android Keystore + AES-256-GCM), numéros masqués par défaut.
- Limite de sécurité **de l'application** : 3 dossiers réels par 24 h (configurable) — présentée
  comme une règle WhAlert, pas une limite officielle WhatsApp. Impossible de multiplier
  artificiellement un même signalement.
- **Console privée** : biométrie si disponible, sinon PIN avec anti brute-force et expiration
  de session ; statistiques, journaux techniques, état réseau/services. Ne permet aucune campagne
  de signalements.
- Pages « Guide de sécurité » et « Ce que cette application peut et ne peut pas faire ».

## Ce qu'elle ne fait PAS (et le dit à l'utilisateur)
- Vérifier l'existence, l'état, un bannissement ou une suspension d'un compte WhatsApp
  (aucune API publique ne le permet).
- Envoyer automatiquement des signalements à WhatsApp, spammer, contourner des limites,
  des CAPTCHA ou des protections.
- Garantir une décision de modération — la décision appartient uniquement à WhatsApp.

## Build & APK release
```bash
./gradlew clean assembleRelease            # APK signé si keystore fourni
./gradlew :app:testReleaseUnitTest         # 23 tests unitaires JVM
```
APK produit : `app/build/outputs/apk/release/app-release.apk`
(minSdk 24, targetSdk 35, R8+shrinkResources, signature APK Scheme v2).

### Signature release
Fournir via variables d'environnement ou `local.properties` (jamais committé) :
`WHALERT_KEYSTORE_PATH`, `WHALERT_KEYSTORE_PASSWORD`, `WHALERT_KEY_ALIAS`, `WHALERT_KEY_PASSWORD`.
Sans keystore, Gradle signe avec la clé debug (build local uniquement).

### Backend optionnel (HTTPS réel uniquement)
Aucun backend n'est préconfiguré (aucune URL fictive). Pour activer la synchronisation
distante, déployez votre propre service HTTPS puis :
```bash
export WHALERT_API_BASE_URL="https://votre-domaine-reel"   # https obligatoire
```
Sans cela, `REMOTE_ENABLED=false` et l'UI indique honnêtement que la synchro est indisponible.
`network_security_config.xml` interdit tout trafic clair en production.

## CI GitHub Actions
`.github/workflows/android-build.yml` : build release, tests unitaires, signature
(keystore via secrets, sinon keystore CI jetable), artefact **app-release.apk téléchargeable**.

## Installation
1. Télécharger `app-release.apk` (artefact Actions ou `app/build/outputs/apk/release/`).
2. Transférer sur le téléphone, autoriser « sources inconnues », installer.
   Ou : `adb install app-release.apk`.

## Documentation
- `BUILD_REPORT.md` — rapport de build exact (taille, SHA-256, signature, tests, limitations)
- `SIZE_REPORT.md` — justification de taille (aucun gonflage artificiel)
- `THIRD_PARTY.md` — bibliothèques, recherches GitHub comparatives, licences

## Architecture
MVVM léger : `ui/` (Compose screens + `WhAlertViewModel`), `core/` (validation, réseau),
`data/` (`SecureStore` Keystore, `RemoteClient` OkHttp, `ReportTextGenerator`,
`OfficialChannels`). Coroutines partout, zéro I/O sur le thread UI.

## Sécurité
- Aucun secret dans Git (vérifié) ; sécrets via env/local.properties non suivis.
- Logs sans données personnelles ; PII chiffrée au repos.
- Permissions minimales : INTERNET, ACCESS_NETWORK_STATE, USE_BIOMETRIC.
- Validation stricte des entrées ; protection injection (pas de SQL, JSON typé kotlinx).

## Tests
Unitaires JVM exécutés : validation numéros (8), domaine/limite/masquage (7), réseau
succès/erreur/timeout/hors-ligne/désactivé via MockWebServer (5), génération de texte (3).
Tests UI Compose fournis dans `androidTest` (exécution nécessitant appareil/émulateur).
