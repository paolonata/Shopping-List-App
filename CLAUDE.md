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
    Transizione tra schermate con `Crossfade`.

### Aggiunta (additiva, mai distruttiva)
- `ShoppingListRepository.addParsedItems` è **solo additivo**: unisce i duplicati
  non spuntati (somma le quantità) e inserisce i nuovi; **non cancella mai** nulla.
  Sia l'aggiunta manuale sia l'import da WhatsApp passano da `addItemsFromText`
  → `addParsedItems`, quindi importare da WhatsApp dopo un'aggiunta manuale (o
  importare più volte) non sovrascrive ciò che c'è già. Test:
  `ShoppingListRepositoryTest` (incluso "manual items are preserved when later
  importing from whatsapp").
- **Aggiunta manuale inline** (NON più pop-up): "Aggiungi manualmente" attiva
  `inlineAdding` → `InlineAddRow` (una riga con `BasicTextField` in fondo alla
  lista, auto-focus). Invio/✓ = aggiunge e resta aperta per il successivo; ✗ (o
  invio a vuoto) chiude. Scroll automatico in fondo mentre si aggiunge.
- **Azioni in basso** (`Scaffold.bottomBar`, `BottomActions`): due bottoni
  rettangolari affiancati (stessa larghezza), sempre visibili: **"Aggiungi
  manualmente"** (inline) e **"Aggiungi da WhatsApp"** (`AddFromTextScreen`),
  identici per stile/gradiente, testo su 2 righe con "Aggiungi" allineato.
- **Condivisione**: icona Share nell'header (quando ci sono articoli) → `ACTION_SEND`
  text/plain con la lista come **elenco puntato** (`• articolo ×N`), via
  `Intent.createChooser` (WhatsApp incluso).
- **Limite di WhatsApp (non dell'app)**: per i messaggi di *testo* WhatsApp offre
  solo "Copia"/"Inoltra", NON "Condividi" (share sheet di sistema). Quindi il
  flusso testo è: Copia in WhatsApp → apri app → Incolla. L'`ACTION_SEND` funziona
  quando un'app espone davvero il testo allo share sheet.

### Quantità, modifica ed editing degli articoli
- **Quantità in aggiunta**: sia `InlineAddRow` (aggiunta manuale) sia le card di
  anteprima in `AddFromTextScreen` (import WhatsApp) hanno uno **stepper -/+**
  (`QuantityStepper`, duplicato nei due file per semplicità). Default **1**,
  modificabile prima di confermare.
  - Nell'anteprima WhatsApp le modifiche di quantità sono tenute in una mappa
    `quantityOverrides` (chiave `nome|nota`) che sopravvive al ricalcolo del
    parser mentre l'utente scrive; `onConfirm` ora riceve la **lista già
    interpretata** (`List<ParsedItem>`, non più il testo grezzo) così le
    modifiche manuali non vengono perse ri-parsando da zero
    (`ShoppingListViewModel.addParsedItems`).
- **Modifica di un articolo già in lista**: tap sul **testo** della card (non
  sulla spunta, non sulla `x`) entra in modalità modifica inline
  (`EditItemRow`): campo nome + stepper quantità + ✓/✗. Funziona sia in "Da
  comprare" sia in "Nel carrello". `Repository.updateItemDetails` (ignora nomi
  vuoti) + `ViewModel.updateItem`.
- **Riordino con drag & drop**: solo nella sezione "Da comprare" (i comprati non
  si riordinano). Icona "maniglia" (`Icons.Default.DragHandle`) a fianco di ogni
  articolo: `detectDragGesturesAfterLongPress` + calcolo manuale dello scambio
  in base all'altezza della card (misurata con `onGloballyPositioned`), niente
  libreria esterna (non verificabile in sandbox). Implementato con una `Column`
  semplice dentro un singolo `item { }` della `LazyColumn` esterna (non con
  `items()` lazy), per tenere la logica di drag disaccoppiata dalla
  virtualizzazione. Al rilascio, `onReorder` passa la lista riordinata a
  `Repository.reorderItems`, che riassegna `position` (0..N-1) in una
  transazione (`@Transaction` su un metodo default del DAO).

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

### Notifica persistente ("lista senza sbloccare il telefono")
L'utente voleva vedere/spuntare la lista senza sbloccare il telefono. **Vincolo
reale di Android**: dopo Android 5 non esistono più i widget sulla schermata di
blocco (a differenza di iOS) — non è una limitazione dell'app. Chiarito con
l'utente via `AskUserQuestion`; ha scelto la **notifica persistente** (l'altra
opzione scartata era il widget in home screen).
- `notification/NotificationPrefs.kt` — flag on/off in `SharedPreferences`
  (per-utente, non richiede DB).
- `notification/ShoppingListNotifier.kt` — costruisce la notifica con
  **`RemoteViews` custom** (`res/layout/notification_list.xml` +
  `notification_row.xml`, un `addView` per riga, max 6 righe + "+N altri"):
  è l'unico modo per avere **più righe cliccabili singolarmente** in una
  notifica di sistema (gli `Action` standard sono troppo pochi/larghi, gli
  stili Inbox/Messaging non hanno tap per riga). `DecoratedCustomViewStyle` +
  `setCustomBigContentView`, canale `IMPORTANCE_LOW` (niente suono ad ogni
  aggiornamento), `setVisibility(PUBLIC)` per essere leggibile sul lock screen
  se l'utente ha attivato le notifiche lì.
- `notification/ShoppingListActionReceiver.kt` — `BroadcastReceiver` **dichiarato
  nel Manifest** (non dinamico) così riceve il tap anche ad app completamente
  chiusa; usa `goAsync()` + coroutine `Dispatchers.IO` per poter chiamare il
  repository (Room `suspend`) da `onReceive`. Ogni riga apre in broadcast con
  un `data Uri` univoco (`shoppinglist://item/<id>`) per evitare che Android
  collassi PendingIntent con extra diversi ma stessa action.
- Icone notifica: vector drawable stencil scritti a mano (`ic_notification.xml`,
  `ic_notif_row_unchecked.xml`) — nessuno script di generazione necessario,
  bastano semplici `pathData` (icone Material standard "check_circle" e
  "radio_button_unchecked").
- **Limiti onesti (documentati anche in UI/comportamento, non solo qui)**:
  dalla notifica si può solo **spuntare** un articolo (sparisce dalla lista
  visibile in notifica); per togliere la spunta bisogna aprire l'app. Se
  l'utente fa "Forza arresto" sull'app, Android blocca anche il
  `BroadcastReceiver` finché non la riapre (limite di sistema, non risolvibile).
  Serve il permesso `POST_NOTIFICATIONS` (richiesto a runtime su Android 13+,
  gestito con `rememberLauncherForActivityResult` nel toggle del menu).
  Toggle: voce "Promemoria in notifica" nel menu ⋮ dell'header (icona
  campanella piena/vuota secondo lo stato).

### Fix posizionamento del menu ⋮
Il `DropdownMenu` collegato all'icona `MoreVert` (in alto a destra) si apriva
percepito "a sinistra"/scollegato dall'icona. Fix: avvolgere **solo**
`IconButton` + `DropdownMenu` in un `Box` dedicato (pattern Material standard
per l'ancoraggio dei popup), invece di lasciarli come semplici fratelli dentro
la `Row` dell'header.

## 3. Stile / design

Look **vibrante e moderno** (l'utente ha bocciato la prima versione bianca/rossa
piatta come "sciapa"):
- **Gradiente di brand** corallo→lampone `#FF6A5E → #F5325B` (in
  `theme/Theme.kt`, esposto da `brandGradient()`), usato **con parsimonia**:
  solo su **FAB**, empty state e pulsante di conferma. Primary vibrante
  `#F5325B` (dark: `#FF7286`). **Niente dynamic color**.
- **Header piatto** (NON a gradiente): l'utente ha bocciato il primo header
  "hero" a gradiente come "orrenda pillola colorata" (troppo acceso in dark).
  Ora è titolo su sfondo normale (`onBackground`), testo "X di Y nel carrello"
  in grigio e `LinearProgressIndicator` sottile in `primary` come unico accento.
  Lezione: vivacità nei piccoli accenti (FAB, pill, spunte, header di sezione),
  non in grandi blocchi colorati.
- Articoli come **card arrotondate** (`Surface` shape 16dp + `shadowElevation`)
  su sfondo grigio chiaro, con spaziatura; niente più righe piatte con divider.
- Checkbox **circolari**: `Icons.Outlined.RadioButtonUnchecked` →
  `Icons.Filled.CheckCircle` (primary). Quantità in **pill** colorata
  (primary @12% alpha). FAB rotondo a gradiente con ombra.
- Storia: prima versione Todoist "pulita" (commit `c080283`) giudicata troppo
  timida → restyle vibrante con gradiente.
- **Font Open Sans**: `.ttf` statici (Regular/SemiBold/Bold/ExtraBold) scaricati
  da GitHub (`googlefonts/opensans`, cartella `fonts/ttf`) e **bundle** in
  `app/src/main/res/font/` (nomi lowercase con underscore). Applicato a TUTTA la
  `Typography` in `Theme.kt`. NB: Open Sans **non ha uno static "Medium"** → nel
  `FontFamily` il peso Medium è mappato sul Regular (esplicito). Prima era
  Montserrat (rimosso su richiesta utente).
- **Sfondo caldo** (l'utente ha bocciato il grigio Android): background carta
  `#FBF7F4` (dark `#16130F`), superfici bianche/scure; grigi tenui caldi.
- **Animazioni/dinamicità**: `Modifier.animateItem()` sulle card (si riposizionano
  quando spunti un articolo e passa da "Da comprare" a "Nel carrello"); checkbox
  con colore animato (`animateColorAsState`) e piccolo "pop" (`animateFloatAsState`
  + spring bouncy su `graphicsLayer` scale); testo che sfuma di colore; barra di
  avanzamento animata; FAB con entrata in scala; `Crossfade` tra schermate.

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

4. **`Unresolved reference 'RESULTS_RECOGNITION'`** (feature dettatura vocale).
   - Causa: `RESULTS_RECOGNITION` è una costante di `SpeechRecognizer` (bundle del
     `RecognitionListener`), NON di `RecognizerIntent`. Per leggere il risultato
     dell'Activity `ACTION_RECOGNIZE_SPEECH` la chiave giusta è
     `RecognizerIntent.EXTRA_RESULTS`. → **Fix**: usare `EXTRA_RESULTS`.

### Cronologia milestone (commit verdi)
- `bb9f869` — prima build verde (app base funzionante).
- `c080283` — restyle "Todoist pulito" (poi giudicato troppo timido/"sciapo").
- `d7cd568` — dettatura vocale + restyle vibrante (gradiente, card, FAB).
- `1a5b77e` — header piatto: rimossa la "pillola" a gradiente in alto, troppo
  accesa in dark; gradiente tenuto solo su FAB/empty state/pulsante conferma.

Andamento del design (utile per capire i gusti dell'utente): piatto bianco/rosso
= "sciapo" → Todoist pulito = ancora timido → gradiente ovunque/header hero =
"orrenda pillola colorata" → **equilibrio**: sfondo/testo puliti + accenti
vivaci piccoli (FAB, spunte, pill quantità, barra avanzamento, header sezione).

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
- L'output di `list_workflow_runs` è **enorme** e supera il limite di token: viene
  salvato su file dal tool. Le righe sono troppo lunghe per `Read` con
  offset/limit → parsarlo con **python/json** (`json.load(open(path))`,
  `o['workflow_runs'][0]` → `head_sha`, `status`, `conclusion`, `id`, `html_url`).
  Usare sempre `per_page=1`.
- Per il polling ripetuto della CI (quando l'utente dice "controlla ogni tot"):
  usare `ScheduleWakeup`; il minimo è **~60s** (non si scende a 30s). Ricordarsi
  di fermare il loop (`stop:true`) a build verde.
- Firma i commit come da istruzioni di sessione (Co-Authored-By + Claude-Session).
