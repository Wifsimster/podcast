# 🌶️ Carne — Recherche de fonctionnalités multi-personas

> Rapport de recherche produit · 2026-06-06
> Méthode : 5 personas utilisateurs distincts ont audité le code existant de
> l'app (Kotlin / Jetpack Compose / Media3 / Room) pour proposer des
> fonctionnalités **nouvelles** (sans doublonner l'existant), puis synthèse
> pondérée en un **Top 10**.

---

## 1. Méthodologie

Cinq « chercheurs produit » ont chacun incarné un profil d'utilisateur réel,
exploré la base de code (`app/src/main/java/com/carne/podcast/`) pour
cartographier ce qui existe déjà, et proposé 6 à 8 idées chacun, avec
estimation d'effort et note d'implémentation ancrée dans de vrais fichiers.

| # | Persona | Angle | Préoccupation principale |
|---|---------|-------|--------------------------|
| 👤 | **Marc, le navetteur** | Voiture / Android Auto / mains-libres | File d'attente, hors-ligne, zéro manipulation au volant |
| 👤 | **Salima, l'accessibilité** | TalkBack, malvoyance, i18n | Accessibilité & localisation (FR/EN) |
| 👤 | **Theo, le minimaliste privacy** | Données, pas de cloud | Possession des données, sauvegarde/restauration |
| 👤 | **Inès, la power-user audiophile** | Contrôle audio avancé | Queue, chapitres, marque-pages, profils par podcast |
| 👤 | **Lina, l'exploratrice** | Découverte / onboarding / polish | Première impression, show-notes, widgets |

**Pondération du Top 10** : consensus inter-personas × impact utilisateur ×
faisabilité × alignement avec l'ADN de l'app (sans pub, sans compte, sans
tracker, local-first).

---

## 2. 🏆 Top 10 des fonctionnalités recommandées

### #1 — File d'attente « Up-Next » persistante et réordonnable
**Personas : Marc + Inès (consensus fort) · Effort : L · Impact : ⭐⭐⭐⭐⭐**

Une vraie file gérée par l'utilisateur : « Lire ensuite », « Ajouter à la
file », glisser-déposer pour réordonner, **conservée au redémarrage**.

> *Aujourd'hui il n'y a pas de file réelle.* `PlaybackConnection.play()`
> dérive une séquence éphémère en faisant `queue.dropWhile { it.id != … }`
> sur la liste visible (`latest`), perdue à la fermeture de l'app. C'est le
> **manque n°1** cité par deux personas indépendamment.
>
> **Implémentation :** nouvelle `QueueItemEntity` (+ `QueueDao`) dans
> `data/local`, construction de la timeline ExoPlayer depuis la file dans
> `PlaybackConnection`, écran « File » + entrées dans le menu long-press
> d'`EpisodeRow`. Exposer aussi un dossier « File » dans Android Auto
> (`PlaybackService.onGetChildren`).

---

### #2 — Export/Import OPML + sauvegarde/restauration locale complète
**Persona : Theo · Effort : S→M · Impact : ⭐⭐⭐⭐⭐ · Aligné ADN**

Exporter ses abonnements en `.opml` standard (migration AntennaPod/gPodder),
les réimporter en masse, et une sauvegarde JSON complète (abonnements +
positions de lecture + réglages) — **100 % local, aucun serveur**.

> *Inexistant aujourd'hui :* `RssParser` ne gère que les flux RSS, aucune
> UI de backup. Pire, la base utilise `fallbackToDestructiveMigration()` →
> **une montée de schéma efface toute la bibliothèque** (voir aussi le
> correctif fondamental en §4).
>
> **Implémentation :** `OpmlExporter`/`OpmlParser` (style `XmlPullParser`),
> sérialisation des DAO + `CarneSettings` en JSON via le sélecteur de
> fichiers système (`ACTION_CREATE_DOCUMENT` / `ACTION_GET_CONTENT`), section
> « Vos données » dans `SettingsScreen`.

---

### #3 — Localisation française (i18n complète)
**Persona : Salima · Effort : L (surtout du balayage) · Impact : ⭐⭐⭐⭐⭐**

Externaliser **toutes** les chaînes UI dans `strings.xml` et ajouter
`values-fr/strings.xml`.

> *Constat fort :* `res/values/strings.xml` ne contient **que** `app_name` —
> zéro `stringResource` dans tout `ui/`, et **aucun `values-fr/`**. Or
> l'émission phare pré-installée (**Silicon Carne**) est française : un
> utilisateur FR (et TalkBack) entend une interface 100 % anglaise. Fondation
> sur laquelle s'appuient toutes les autres features.
>
> **Implémentation :** extraire ~80 littéraux de `ui/screens/*` et
> `ui/components/*`, paramétrer les libellés interpolés (durées, « Forward
> 30s ») avec `plurals`, puis traduire.

---

### #4 — Chapitres (parsing + navigation)
**Personas : Marc + Inès (consensus) · Effort : L · Impact : ⭐⭐⭐⭐**

Parser `<podcast:chapters>` (JSON/PSC/ID3), afficher une liste de chapitres
cliquables + titre du chapitre courant sur le lecteur, la notification et le
casque auto (commandes next/prev).

> *Inexistant :* `RssParser` ne capture aujourd'hui ni chapitres ni
> transcripts. Pour des épisodes longs en voiture, seul le saut à intervalle
> fixe (10s/30s) existe.
>
> **Implémentation :** extension `RssParser` + champ chapitres sur
> `EpisodeEntity`, fetch/parse JSON dans le repository, UI de seek dans
> `PlayerScreen`.

---

### #5 — Saut des silences + normalisation/boost du volume
**Personas : Marc + Inès (consensus) · Effort : M · Impact : ⭐⭐⭐⭐**

« Skip silence » et boost de volume pour la parole, essentiels contre le
bruit de la route/du train et pour récupérer les épisodes mal masterisés.

> *Inexistant :* le player ExoPlayer est construit avec de simples
> `AudioAttributes`, sans aucun `AudioProcessor`. Le contenu est pourtant
> déjà typé `AUDIO_CONTENT_TYPE_SPEECH`.
>
> **Implémentation :** `setSkipSilenceEnabled(true)` +
> `SilenceSkippingAudioProcessor`/`SonicAudioProcessor`/`LoudnessEnhancer`
> via un `DefaultRenderersFactory` dans `PlaybackService.onCreate()`, exposés
> en réglages (`SettingsRepository`).

---

### #6 — Show-notes riches et cliquables (HTML + horodatages)
**Personas : Lina + Salima + Inès (consensus fort) · Effort : M · Impact : ⭐⭐⭐⭐**

Rendre les notes d'épisode en texte formaté avec **liens cliquables** et
**timestamps `hh:mm:ss` tappables** qui font sauter le lecteur.

> *Constat :* `stripHtml()` (`Format.kt:34`) **détruit** le HTML — liens,
> paragraphes et chapitres perdus, dans `PlayerScreen` comme `PodcastScreen`.
> Ça « fait cassé » face à Apple/Pocket Casts, et c'est illisible au
> lecteur d'écran.
>
> **Implémentation :** remplacer `stripHtml` par un `AnnotatedString` issu de
> `HtmlCompat.fromHtml`, + regex d'horodatage → seek. Conserver `stripHtml`
> uniquement pour les aperçus une-ligne (`EpisodeRow`).

---

### #7 — Profils de lecture par podcast
**Personas : Inès + Marc · Effort : M · Impact : ⭐⭐⭐⭐**

Surcharger par émission : vitesse, intervalles de saut, skip-silence,
auto-download — appliqués automatiquement au chargement d'un épisode.

> *Constat :* `defaultSpeed`/`skipBackMs`/`skipForwardMs` sont **globaux** ;
> régler la vitesse dans le lecteur **écrase le défaut global** pour tout. On
> veut l'actu à 2× et le récit à 1,2×.
>
> **Implémentation :** colonnes nullables d'override sur `PodcastEntity`,
> lues dans `PlaybackConnection.play()` par `feedUrl` ; cesser d'écrire la
> vitesse du lecteur dans le défaut global.

---

### #8 — Auto-téléchargement des nouveaux épisodes
**Persona : Marc · Effort : M · Impact : ⭐⭐⭐⭐**

Bascule « télécharger automatiquement les nouveaux épisodes » par podcast,
pour qu'ils soient prêts **avant** le trajet (zones blanches du métro).

> *Constat :* la détection des nouveautés existe déjà
> (`FeedRefreshWorker` + `NewEpisodeNotifier`) mais le téléchargement reste
> 100 % manuel.
>
> **Implémentation :** flag `autoDownload` sur `PodcastEntity` ;
> dans `FeedRefreshWorker`, appeler `DownloadManager.enqueue()` sur le
> `NewEpisodeBatch`, en respectant le `wifiOnlyDownloads` existant.

---

### #9 — Onboarding « choisis tes centres d'intérêt »
**Persona : Lina · Effort : M · Impact : ⭐⭐⭐**

Flux de premier lancement : taper 2-4 thèmes → abonnement en un tap aux
meilleures émissions, pour remplir l'écran d'accueil dès la première minute.

> *Constat :* pas de vrai onboarding, juste un empty-state. Tout l'outillage
> existe déjà (chips de `PodcastThemes`, `PodcastSearchService.topPodcasts()`,
> `repository.subscribe()`).
>
> **Implémentation :** écran `onboarding` routé avant `HOME` dans
> `CarneNavigation`, gardé par un flag DataStore `onboarding_done`.

---

### #10 — Lot accessibilité : sémantique fusionnée + actions personnalisées
**Personas : Salima + Lina · Effort : S→M · Impact : ⭐⭐⭐ · Gains rapides**

Fusionner la sémantique des lignes pour TalkBack (`mergeDescendants`),
exposer les actions cachées (« Marquer comme lu », « Télécharger ») en
`CustomAccessibilityAction`, et progrès de téléchargement vocalisé. Inclure
le **filtre/recherche d'épisodes au sein d'un podcast** (gain rapide « S »
réclamé par Lina, absent de `PodcastScreen`).

> **Implémentation :** `mergeDescendants` sur `EpisodeRow`/ligne de recherche,
> `semantics { customActions = … }` réutilisant les lambdas existantes,
> `stateDescription` sur la progression, et un `OutlinedTextField` filtrant la
> liste dans `PodcastViewModel`/`PodcastScreen`.

---

## 3. ⚠️ Correctif fondamental à traiter en parallèle

**Migrations Room sûres (anti perte de données).** La base est aujourd'hui en
`fallbackToDestructiveMigration()` avec `exportSchema = false`
(`AppModule.kt`, `CarneDatabase.kt`) : **toute mise à jour modifiant le schéma
efface la bibliothèque et l'historique de l'utilisateur.** Plusieurs features
ci-dessus (queue, chapitres, profils, marque-pages) ajoutent des colonnes/tables
— il faut donc activer `exportSchema = true` et écrire de vraies `Migration`
avant de les livrer. *Pré-requis technique du Top 10.*

---

## 4. 🥈 Mentions honorables (hors Top 10)

- **Marque-pages horodatés avec notes** (Inès) — distinct du simple
  `positionMs`. Effort M.
- **Reprise auto à la connexion Bluetooth / Android Auto** (Marc) — démarrer
  le dernier épisode quand la voiture se connecte. Effort M.
- **Widget écran d'accueil (Glance)** « Continue listening » (Lina). Effort L.
- **Gestionnaire de stockage** : vue de l'espace total + nettoyage en masse +
  fichiers orphelins (Theo). Effort M.
- **Partage de clip / lien horodaté** (Inès) — version « S » : intent de
  partage avec timestamp ; version « L » : vrai découpage via Media3
  `Transformer`.
- **Vue transcript synchronisée** `<podcast:transcript>` avec tap-to-seek
  (Inès). Effort L.
- **Listes intelligentes / filtres sauvegardés** (« téléchargés & non lus »,
  « en cours ») (Inès). Effort M.
- **Carrousel « Fresh picks » + badge « Nouveau »** sur l'accueil (Lina).
  Effort S-M.
- **Transparence/contrôle de la sauvegarde Android** (`allowBackup` envoie
  aujourd'hui la base vers le cloud Google — ironique pour une app no-cloud)
  (Theo). Effort S.
- **Section Accessibilité dans les réglages** : fort contraste + réduire les
  animations (Salima). Effort M.

---

## 5. ✅ Recommandation

Si une seule fonctionnalité devait être priorisée : **#1 la file d'attente
persistante** — c'est le seul manque cité spontanément par deux personas
opposés (navetteur *et* audiophile), à fort impact quotidien, et qui
débloque ergonomiquement la voiture comme la curation avancée.

**Séquencement conseillé :**
1. Correctif fondamental (migrations Room sûres) — *protège tout le reste.*
2. Quick wins à faible effort : **#10** (filtre épisodes + a11y), #2 export
   OPML (S), badge « Nouveau ».
3. Chantier phare : **#1 file d'attente**, puis **#6 show-notes riches** et
   **#3 localisation FR**.
4. Différenciation audio : **#4 chapitres**, **#5 skip-silence**, **#7
   profils par podcast**.
