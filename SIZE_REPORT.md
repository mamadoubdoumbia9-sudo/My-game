# WhAlert — SIZE_REPORT.md

## Taille réelle de l'APK release : 2,6 Mo (2 595 052 octets)

L'objectif indicatif de 90 Mo minimum **n'est pas atteint**, volontairement.
Conformité à la règle absolue du cahier des charges :

> « Si l'application réelle nécessite naturellement moins de 90 Mo, NE PAS fabriquer
> 90 Mo de données inutiles. » / « INTERDICTION ABSOLUE de gonfler artificiellement le fichier. »

**Le contenu fonctionnel réel ne justifie pas une taille de 90 Mo ; aucune donnée
artificielle n'a été ajoutée.** Aucun fichier vide, aucune image inutilisée, aucune
vidéo, aucun asset factice, aucun code mort, aucune bibliothèque décorative.

## Décomposition réelle du contenu (vérifiée sur l'APK produit)

| Élément | Nature | Utilisation réelle |
|---|---|---|
| classes.dex + classes2.dex (~1,4 Mo après R8) | Code Kotlin compilé : app WhAlert + OkHttp + kotlinx.serialization + libphonenumber (partiellement gardé par R8) + Coil + Compose runtime/matériel | Chaque classe est atteignable depuis les écrans/logique testés |
| AndroidManifest.xml, resources.arsc, drawables/mipmaps | Icône adaptative vectorielle (logo bouclier WhAlert), thème Material 3, network_security_config | Affichées à l'écran d'accueil et dans l'app |
| Fichiers de localisation AndroidX/Compose | values-*/ inclus par les bibliothèques | Traductions système réelles (non supprimables sans casser les libs) |
| META-INF (signature) | APK Signature Scheme v2 | Vérification d'intégrité |

R8 + `isShrinkResources = true` réduisent au strict nécessaire : c'est la signature
d'une application saine, pas d'une application incomplète (7 écrans Compose, navigation,
stockage chiffré Keystore, validation E.164, console privée biométrique, client HTTPS,
tests — tous réellement implémentés).

## Ressources documentées (obligation « chaque ressource réellement utilisée »)

| Nom | Type | Taille | Utilisation | Pourquoi nécessaire |
|---|---|---|---|---|
| ic_launcher_foreground.xml | Vector drawable | ~3 Ko | Logo accueil + launcher | Identité visuelle de l'app |
| ic_launcher.png (mdpi→xxxhdpi) | Bitmap adaptive icon | ~15 Ko total | Launcher legacy | Compatibilité pré-API 26 |
| network_security_config.xml | XML config | <1 Ko | Force HTTPS/TLS global | Exigence sécurité du projet |
| Polices/icônes Material (via material-icons-extended, tree-shaké par R8) | Vecteurs compilés | inclus dans dex | Boutons, timeline, catégories | UI complète |

Aucune autre resource n'est embarquée : pas d'assets audio/vidéo, pas de modèles ML
(l'app n'en utilise pas), pas de traductions factices.

## Comparaison honnête
Une application Compose + Material 3 minimale fait typiquement 2–5 Mo. WhAlert est à
2,6 Mo avec toutes ses fonctionnalités car R8 optimise fortement. Un APK ≥ 90 Mo
nécessiterait d'ajouter des composants que l'app n'utilise pas (NDK natif, modèles
offline, médias) — ce qui violerait la règle anti-gonflage.
