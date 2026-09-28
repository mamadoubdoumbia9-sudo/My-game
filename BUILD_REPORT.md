# WhAlert — BUILD_REPORT.md

Nom :
WhAlert

Package :
com.whalert.app

Version :
1.0.0 (versionCode 1)

Version Android minimale :
API 24 (Android 7.0)

Target SDK :
API 35 (Android 15), compileSdk 35

Taille APK :
2,6 Mo (2 595 052 octets) — voir SIZE_REPORT.md pour la justification d'honnêteté de la taille

SHA-256 APK :
6809cb190be9cc67e0123f80bf813ddb9c942d3ae6d97d3da3070378944fba0e

Signature :
VALID — vérifiée par `apksigner verify` (signataire CN=WhAlert Release, SHA-256 du certificat e17df3db…4ede). Keystore de build local/CI non committé ; pour une publication Play Store, régénérer un keystore privé et le fournir via secrets GitHub Actions.

Manifest :
VALID — `aapt dump badging` retourne package='com.whalert.app', permissions minimales : INTERNET, ACCESS_NETWORK_STATE, USE_BIOMETRIC (+ USE_FINGERPRINT legacy). Aucune permission dangereuse.

Installation :
NON TESTÉE — aucun émulateur/appareil Android disponible dans cet environnement. Compilation et validation statique de l'APK (structure ZIP, classes.dex ×2, ressources, signature) vérifiées réellement.

Lancement :
NON TESTÉ sur appareil (voir ci-dessus). Le code passe les tests unitaires JVM et la compilation R8 release.

Tests :
`./gradlew :app:testReleaseUnitTest` → BUILD SUCCESSFUL — 23 tests, 0 échec :
- PhoneNumberValidatorTest : 8/8 (E.164 valide/invalide, +223…, indicatifs, numéros courts)
- DomainModelTest : 7/7 (statuts dossier, limite 3/24 h, masquage numéro, anti-multi-envois)
- RemoteClientNetworkTest : 5/5 (succès HTTP, erreur 5xx, timeout, connexion coupue, backend désactivé — MockWebServer réel sur TLS/HTTP in-process, jamais expédié)
- ReportTextGeneratorTest : 3/3 (texte factuel, sans accusation inventée, structure officielle)
Tests UI Compose (`androidTest`, ui-test-junit4 + Espresso) : compilables, exécution nécessitant un appareil/émulateur — NON EXÉCUTÉS ici (honnêteté : non simulés).

Backend :
Aucun backend par défaut. `BuildConfig.API_BASE_URL` est vide au build → `REMOTE_ENABLED=false` → la synchronisation distante affiche honnêtement « indisponible » dans l'UI. Si l'opérateur déploie son propre backend HTTPS et fournit `WHALERT_API_BASE_URL` (variable d'environnement de build, jamais committée), le client OkHttp réel l'utilise en HTTPS strict (network_security_config.xml interdit tout clair).

Services externes :
- Aucun appel réseau sortant automatique. Les canaux de signalement sont les mécanismes OFFICIELS WhatsApp : formulaire officiel https://support.whatsapp.com/hc/requests/new ouvert via ACTION_VIEW (navigateur système), guidage du signalement in-app WhatsApp documenté. Aucune API WhatsApp privée, aucun endpoint inventé.
- E-mail : ouverture du client e-mail système pré-rempli (mailto) uniquement après double confirmation explicite ; aucune automatisation d'envoi, aucun SMTP embarqué.

Fonctionnalités réellement fonctionnelles (dans l'APK) :
- Accueil animé, état de connexion, navigation complète (7 écrans)
- Nouveau signalement : validation E.164 temps réel (libphonenumber), catégories, description, preuves (sélecteur images via SAF + aperçu Coil), date/heure
- Vérification + double confirmation obligatoire avant toute transmission
- Génération du texte de signalement factuel (aperçu, modifier/copier/annuler/envoyer)
- Timeline d'état honnête (brouillon→préparé→soumis→confirmation technique→décision WhatsApp INCONNUE)
- Historique chiffré (EncryptedSharedPreferences/AES-256-GCM via Keystore), numéros masqués
- Limite de sécurité applicative : 3 dossiers/24 h (configurable, affichée comme règle de l'app, pas de WhatsApp)
- Console privée : biométrie si disponible, sinon PIN avec anti brute-force + expiration de session, statistiques locales journalisées
- Page transparence « ce que l'app peut/ne peut pas faire », guide de sécurité
- Gestion réseau réelle : loading/success/error/timeout/retry unique, détection connectivité (ACCESS_NETWORK_STATE)

Fonctionnalités NON disponibles (déclarées honnêtement dans l'app) :
- Vérifier si un compte WhatsApp existe / est banni / suspendu : IMPOSSIBLE publiquement — l'app affiche exactement : « Impossible de confirmer publiquement l'état de ce compte avec les informations disponibles. » et « WhatsApp ne fournit pas de confirmation publique permettant à cette application de vérifier directement si ce compte a été suspendu. »
- Envoi automatique de signalements à WhatsApp : n'existe pas (aucune API publique de signalement) ; l'app prépare le dossier et redirige vers le canal officiel.
- Synchronisation cloud : désactivée tant qu'aucun backend réel n'est configuré.

Limitations :
- Installation/lancement sur appareil non testés dans cet environnement CI conteneurisé (pas de KVM/émulateur).
- Taille APK < 90 Mo : contenu fonctionnel réel insuffisant pour justifier 90 Mo ; aucune donnée artificielle ajoutée (interdiction stricte respectée) — détails dans SIZE_REPORT.md.
- L'app ne peut ni garantir ni connaître une décision de modération WhatsApp ; statut « DÉCISION DE MODÉRATION : Inconnue » par conception.

Contrôle final :
[x] APK généré (app/build/outputs/apk/release/app-release.apk)
[x] APK non vide (2,6 Mo, 938 entrées, classes.dex + classes2.dex)
[x] package com.whalert.app (aapt badging)
[x] AndroidManifest valide
[x] APK signé (apksigner verify VALID)
[x] HTTPS only, aucun localhost/fictif dans le code expédié
[x] Aucune API WhatsApp inventée, aucune simulation d'envoi/bannissement
[x] Tests unitaires exécutés : 23/23 OK
[x] THIRD_PARTY.md, SIZE_REPORT.md, BUILD_REPORT.md présents
[x] GitHub Actions : .github/workflows/android-build.yml (build + signature + artefact app-release.apk téléchargeable)
