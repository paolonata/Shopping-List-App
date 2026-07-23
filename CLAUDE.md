# CLAUDE.md — Memoria del progetto "Lista Spesa da WhatsApp"

Questo file serve a recuperare rapidamente il contesto in sessioni future di
Claude Code. Contiene: cos'è il progetto, com'è fatto, il processo di build,
**cosa ha funzionato e cosa no**, e le note operative su GitHub/CI.

---

## 1. Cos'è

App Android che trasforma il classico messaggio WhatsApp della spesa (testo
libero separato da virgole, senza ordine né checkbox) in una lista della spesa
spuntabile. Nata da un'esigenza reale dell'utente (`pa.nataloni@gmail.com`).

- **Repo GitHub**: `paolonata/Shopping-List-App` (pubblico)
- **Branch di sviluppo**: `claude/android-whatsapp-shopping-list-eoqwc9`
  (è anche il branch di default: il repo è nato vuoto, non esiste `main`)
- **Lingua di comunicazione con l'utente**: italiano.

## 2. Architettura

Progetto Gradle multi-modulo (Kotlin, version catalog in `gradle/libs.versions.toml`):

- **`parser/`** — modulo Kotlin/JVM **puro** (nessuna dipendenza Android).
  - `WhatsAppListParser.parse(text) -> List<ParsedItem(name, quantity, note)>`
  - Gestisce: virgole top-level (NON dentro le parentesi), quantità iniziali
    (`2 pere`), marcatori WhatsApp `*_~`, timestamp di chat esportate, righe
    puntate/numerate, unione dei duplicati sommando le quantità.
  - Ha test JUnit in `parser/src/test/...` — **compilabili e testabili senza
    Android SDK** (`./gradlew :parser:test`). Usare questi per validare la logica.
- **`app/`** — app Android, Kotlin + Jetpack Compose (Material 3).
  - `data/` — Room: `ShoppingItem` (entity), `ShoppingItemDao`,
    `ShoppingListDatabase`, `ShoppingListRepository` (unisce i duplicati non
    ancora spuntati invece di duplicarli).
  - `ui/` — `ShoppingListViewModel`, `HomeScreen`, `AddFromTextScreen`,
    `ui/theme/Theme.kt`.
  - `MainActivity` — gestisce anche l'intent `ACTION_SEND` (text/plain) con cui
    WhatsApp condivide il testo → apre direttamente la schermata di anteprima.

### Input vocale (dettatura)
`AddFromTextScreen` ha un pulsante **🎤 Detta** che usa il riconoscimento
vocale di sistema via `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` (lingua
`it-IT`) attraverso `rememberLauncherForActivityResult`. Il testo trascritto
viene **aggiunto su una nuova riga** e passa nel parser esistente. Niente chiavi
API né costi; su telefoni moderni funziona offline. Manifest: serve il blocco
`<queries>` per `android.speech.action.RECOGNIZE_SPEECH` (package visibility su
Android 11+). NB: una singola dettatura continua torna senza virgole → il
consiglio all'utente è dettare **un articolo alla volta** (oppure editare le
virgole a mano; l'anteprima è live). Approcci scartati per ora: Whisper
on-device (APK enorme, build nativa non testabile in locale) e STT cloud
(chiave API + privacy).

## 3. Stile / design

Look **vibrante e moderno** (l'utente ha bocciato la prima versione bianca/rossa
piatta come "sciapa"):
- **Gradiente di brand** corallo→lampone `#FF6A5E → #F5325B` (in
  `theme/Theme.kt`, esposto da `brandGradient()`), usato su **header "hero"**,
  **FAB** ed empty state. Primary vibrante `#F5325B` (dark: `#FF7286`).
  **Niente dynamic color**.
- **Header hero** a gradiente con titolo, testo "X di Y nel carrello" e
  `LinearProgressIndicator` bianco; angoli inferiori arrotondati.
- Articoli come **card arrotondate** (`Surface` shape 16dp + `shadowElevation`)
  su sfondo grigio chiaro, con spaziatura; niente più righe piatte con divider.
- Checkbox **circolari**: `Icons.Outlined.RadioButtonUnchecked` →
  `Icons.Filled.CheckCircle` (primary). Quantità in **pill** colorata
  (primary @12% alpha). FAB rotondo a gradiente con ombra.
- Storia: prima versione Todoist "pulita" (commit `c080283`) giudicata troppo
  timida → restyle vibrante con gradiente.

### Icona
- Generata via script Python con **supersampling** (nessun tool grafico
  installato nell'ambiente): quadrato arrotondato rosso + checklist bianca con
  la prima voce spuntata. Lo script vive nella scratchpad di sessione (non
  committato); se serve rigenerarla, ricrearlo scrivendo PNG RGBA a mano
  (struct+zlib) nelle cartelle `app/src/main/res/mipmap-*dpi/`
  (`ic_launcher.png` e `ic_launcher_round.png`), dimensioni 48/72/96/144/192.
- Verificare sempre l'icona aprendo il PNG generato PRIMA di committare.

## 4. Vincoli dell'ambiente (IMPORTANTE)

L'ambiente sandbox di Claude Code **NON ha l'Android SDK** e **NON ha accesso a
Google Maven** (`dl.google.com` → 403 dal proxy). Conseguenze:
- **Non si può compilare il modulo `app` in locale.** Si valida solo il
  modulo `parser` con Gradle di sistema.
- Il Gradle wrapper va generato altrove: `gradle wrapper --gradle-version X
  --offline` (senza `--offline` fallisce la validazione dell'URL della distro).
- Non usare `jvmToolchain(17)` nel modulo parser: senza toolchain scaricabili
  Gradle fallisce. Lasciare che usi la JVM di sistema (JDK 21 presente).
- **La verifica reale della build `app` avviene SOLO in CI** (GitHub Actions,
  che ha SDK e accesso a Google Maven). Quindi: pushare e leggere i log CI.

## 5. Come ottenere l'APK (per l'utente)

Non serve Android Studio né l'app GitHub sul telefono. La CI produce un APK di
debug come **artifact**:
1. La GitHub Action `Android CI` gira ad ogni push e carica l'artifact
   `app-debug`.
2. L'utente apre la pagina della run su github.com (da **browser**, loggato):
   `https://github.com/paolonata/Shopping-List-App/actions/runs/<RUN_ID>`
3. Sezione **Artifacts** → scarica `app-debug` (è uno **zip**) → estrai
   `app-debug.apk` → installa sul telefono (abilitare "installa da sorgenti
   sconosciute").
4. **Aggiornamenti**: essendo build di debug non firmate in modo stabile, se dà
   "firma non corrispondente / app non installata" → **disinstallare la vecchia
   versione** e reinstallare (i dati locali si perdono, accettabile).

L'app GitHub sul telefono è **facoltativa**: basta github.com da browser.

## 6. CI — cosa ha funzionato e cosa no (storia dei fix)

Workflow: `.github/workflows/android-ci.yml` (job: test + assembleDebug +
upload artifact). `workflow_id` = 319204816.

Problemi incontrati e soluzioni, **in ordine** (utile per non ripeterli):

1. **Le Actions non partivano / non erano elencate.**
   - Causa 1: il repo era nato vuoto, il filtro `on: push: branches: [main]`
     non scattava mai (non esiste `main`). → **Fix**: `on: push:` /
     `pull_request:` senza filtro di branch.
   - Causa 2: GitHub Actions **disabilitate** a livello di repo. → **L'utente**
     deve abilitarle: Settings → Actions → General → "Allow all actions" → Save.
     (Claude non può farlo via API.)

2. **`Compose Compiler Gradle plugin is required` (Kotlin 2.0+).**
   - Da Kotlin 2.0 il compilatore Compose è un plugin a sé. → **Fix**: aggiunto
     `org.jetbrains.kotlin.plugin.compose` (alias `kotlin-compose`) al catalog,
     al root `build.gradle.kts` (apply false) e applicato in `app`.

3. **`Cannot access 'RowColumnParentData?.weight': it is internal`.**
   - Causa vera: **import errato** `androidx.compose.foundation.layout.weight`
     in `AddFromTextScreen.kt`. Dentro un `Row`/`Column` il modificatore
     `.weight()` è già disponibile via il receiver di scope (RowScope/
     ColumnScope) e **non va importato**; l'import esplicito risolveva alla
     proprietà interna. → **Fix**: rimuovere quell'import.
   - **Errore di processo mio**: prima di trovare la causa vera avevo cambiato
     `compose-bom` a `2024.+` pensando fosse un problema di versione. NON lo era.
     Poi ripristinato a `2024.10.00` (pinned = riproducibile). Lezione: leggere
     bene il messaggio del compilatore prima di "indovinare" versioni.

Prima build verde: commit `bb9f869`. Restyle Todoist verde: commit `c080283`.

### Versioni chiave (in `gradle/libs.versions.toml`)
- AGP 8.6.1, Kotlin 2.0.21, KSP 2.0.21-1.0.28, compose-bom 2024.10.00,
  Room 2.6.1. compileSdk/targetSdk 34, minSdk 24. JDK 17 in CI.

## 7. Processo di lavoro consigliato (per sessioni future)

1. Modifica il codice.
2. Se tocchi il **parser**: `./gradlew :parser:test` in locale (usa `gradle` di
   sistema in un progetto temporaneo se il wrapper non scarica la distro).
3. Se tocchi l'**app**: NON puoi compilare in locale → rivedi bene import/API
   Compose (devono esistere nella BOM 2024.10) prima di pushare.
4. Commit sul branch `claude/android-whatsapp-shopping-list-eoqwc9`, push.
5. Leggi la CI: `mcp__github__actions_list` (list_workflow_runs, workflow_id
   319204816) → se fallita, `mcp__github__get_job_logs` con
   `return_content=true`, individua l'errore **reale**, fixa, ripushа.
6. Quando verde: dai all'utente il link della run per scaricare `app-debug`.

### Attenzione operative
- Non creare Pull Request se l'utente non la chiede.
- L'output di `list_workflow_runs` è enorme: filtrare/parsare (es. jq/python) o
  usare `per_page=1`.
- Firma i commit come da istruzioni di sessione (Co-Authored-By + Claude-Session).
