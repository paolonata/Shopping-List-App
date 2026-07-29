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
- **Riordino con drag & drop (fluido)**: solo nella sezione "Da comprare".
  Maniglia (`Icons.Default.DragHandle`) con `detectDragGesturesAfterLongPress`;
  al posto della Column non-lazy della prima versione, gli item sono ora
  espansi come `items(...)` diretti nella `LazyColumn` madre con
  `key = { it.id }` e `Modifier.animateItem()` — così quando l'ordine cambia
  durante il drag, gli altri articoli **scivolano con animazione fluida**
  (prima "saltavano" da una posizione all'altra). L'articolo trascinato ha
  `translationY` = offset del dito + scala 1.03 + shadow 12dp animate con
  spring per feedback tattile. Al rilascio, `onReorder` passa la lista
  riordinata a `Repository.reorderItems`, che riassegna `position` (0..N-1) in
  una transazione (`@Transaction` su un metodo default del DAO). Il threshold
  di scambio è 0.6 * height dell'item (invece di 0.5) per rendere lo scambio
  più prevedibile e meno "nervoso".

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

**Nota importante sull'esperienza reale**: l'utente ha segnalato che "non
funziona / non capisco cosa devo fare". Ci sono due possibili cause:
1. **Non ha attivato il toggle** dal menu ⋮ → "Attiva promemoria in notifica"
   (aggiunto Toast di conferma quando l'attiva/disattiva, per feedback esplicito).
2. **Il produttore del telefono** (Xiaomi/MIUI/HyperOS in particolare) blocca di
   default le notifiche del lock screen per app installate fuori dal Play Store,
   e/o blocca i background broadcast delle app "non frequenti". In quel caso
   serve autorizzare a mano: Impostazioni → App → Lista Spesa → Notifiche →
   Blocco schermo (per farla vedere sul lock screen); e Batteria → No limiti
   (per far arrivare i tap sul BroadcastReceiver). NON è aggirabile via
   Manifest — è un layer specifico del vendor sopra Android.

Il Toast di attivazione già ricorda: "Se non lo vedi a schermo bloccato,
autorizza le notifiche di questa app nelle impostazioni del telefono."

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
- **Approccio notifica leggibile su lockscreen**: NON usiamo più custom
  `RemoteViews` (il system UI sul lockscreen li ignora o li rende in un blob
  di testo poco leggibile). Ora usiamo `NotificationCompat.InboxStyle` per
  righe separate con font di sistema + fino a **3 `NotificationCompat.Action`**
  come pulsanti "✓ Latte", "✓ Pane"... visibili anche sul lock screen. I file
  `res/layout/notification_list.xml`, `notification_row.xml` e
  `drawable/ic_notif_row_unchecked.xml` restano nel repo ma non sono più
  referenziati dal codice (si possono rimuovere in un cleanup futuro).
  Canale attuale: `shopping_list_reminder_v3` (i channel sono immutabili
  post-creazione, quindi ogni cambio settings importante = nuovo channel ID
  con cleanup dei precedenti in `cancel()`).
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

### Barra "aggiungi al carrello" in basso — usabilità (evoluzione)
- Placeholder cambiato da "Scrivi un articolo…" a **"Aggiungi al carrello"** (più
  chiaro sull'azione), stringa `quick_add_hint`.
- **Problema**: quando la tastiera si apre (aggiunta manuale), l'articolo appena
  aggiunto finiva nascosto sotto la tastiera senza scorrimento automatico.
  Primo fix: `LaunchedEffect(items.size)` con `listState.animateScrollToItem(...)`
  che scorreva sempre in cima all'ultimo elemento di "Da prendere" ad ogni
  aggiunta (commit `adc908e`).
- **Feedback successivo**: "sposta un po' verso l'alto la pillola" (poco margine
  dal bordo/tastiera) e "non vedo più la lista di 'Presi'" (lo scroll forzato ad
  ogni aggiunta spingeva via la sezione "Presi", e ripartiva ad ogni nuovo
  articolo scavalcando eventuali scroll manuali dell'utente verso "Presi").
  - **Fix margine**: padding inferiore della `BottomQuickAddBar` aumentato
    (`bottom = 22.dp` invece di `10.dp` uniforme) per più respiro sopra la
    tastiera/bordo schermo.
  - **Fix scroll**: sostituito lo scroll forzato con **`BringIntoViewRequester`**
    per-articolo (uno per ogni item di "Da prendere", tenuto in una
    `mutableStateMapOf<Long, BringIntoViewRequester>`). Un `LaunchedEffect(toBuy)`
    calcola il **diff degli id** rispetto al giro precedente (non solo la
    dimensione) e chiama `requestBringIntoView()` solo sul nuovo articolo, con lo
    **scroll minimo necessario** invece di un salto forzato in cima — se
    l'articolo è già (anche solo parzialmente) visibile non scorre affatto,
    lasciando "Presi" dov'è.

## 3. Stile / design

**Palette attuale (zinc + indigo, "premium neutrale")**: rifatto su richiesta
dell'utente ("colori più neutri, moderna, chiaro e scuro curati"):
- Neutri: famiglia **zinc** (grigi puri, non caldi né freddi, tipo Notion/Linear).
  Light: bg `#FAFAFA`, surface bianca, outline `#E4E4E7`, testo `#18181B`.
  Dark: bg `#09090B`, surface `#18181B`, outline `#3F3F46`, testo `#FAFAFA`.
- Accento: **indigo** (`#4F46E5` light, `#818CF8` dark), con `primaryContainer`
  usato per le pill delle quantità e il cerchio dell'empty state.
- **Niente gradiente rosso corallo/lampone** sui bottoni: preferiti tinte piene.
  La funzione `brandGradient()` esiste ancora ma non è più usata dalla UI (può
  restare per future decorazioni).
- **Bottoni azione in basso**: uno **outlined** ("Aggiungi manualmente") e uno
  **filled indigo** ("Aggiungi da WhatsApp"). Sostituiscono i due bottoni a
  gradiente identici (che confondevano quale fosse l'azione primaria).
- **Card articoli**: `Surface` 14dp shape + bordo sottile (1dp `outlineVariant`)
  + `shadowElevation` piccola. Più raffinato e piatto della versione precedente.
- **Header**: piatto (titolo bold, sottotitolo grigio, progress bar 4dp sottile).
  Le icone Share e MoreVert usano `onSurfaceVariant`.

### Storia del design (per capire i gusti dell'utente)
- v1 "bianco/rosso piatto" → **"sciapo"**.
- v2 "Todoist pulito" (commit `c080283`) → **troppo timido**.
- v3 gradiente corallo→lampone ovunque, header hero → **"orrenda pillola colorata"** (troppo acceso).
- v4 gradiente solo su FAB/empty state/conferma → **quasi giusto**.
- v5 palette **zinc + indigo neutra** → utente: "cambiato colori ma stile è quello, sembra 2015".
- v6 **"Mercato 2026" (attuale)**: font **Space Grotesk** (distintivo,
  Vercel/Stripe-like), palette bianco+nero puri con **accento lime elettrico
  #CCFF00** (associazione "fresco"), tipografia scala Display grande, hero
  header con **numero prominente "5/12 fatti"** invece di barra di progresso,
  **checkbox quadrate arrotondate** (non cerchi) piene lime quando spuntate,
  card con bordo netto invece di ombra, bottoni **pill grandi** (28dp radius)
  con etichette brevi "Nuovo" / "Da WhatsApp". Icona rifatta coerente: quadrato
  lime con checklist nera minimal.
- La lezione: cambiare solo palette non basta, l'utente percepisce "stile 2015"
  se il layout è quello convenzionale. Serve rinnovare **tipografia, forme,
  hierarchy visuale**, non solo i colori.
- v7 **"Editoriale monocromatico" (attuale)**: l'utente ha mandato uno screenshot
  di riferimento (to-do app minimale stile Things/TickTick: bianco/nero puri,
  header piccolo centrato in maiuscolo, righe piatte senza card, checkbox
  circolari, pillole outline per i badge, barra in basso con icona + campo di
  testo pillola invece di bottoni pieni). Ricreato fedelmente:
  - **Palette monocromatica**: `primary` = `onBackground` (nero in light, bianco
    in dark) invece di un colore acceso — il contrasto stesso è l'accento.
    Niente più lime. `brandGradient()` resta ma ora è nero/nero (placeholder,
    quasi inutilizzato).
  - **Header minimale**: `Box` con icone allineate ai bordi (`Alignment.CenterStart/
    CenterEnd`) e titolo centrato in maiuscolo piccolo (`labelLarge`), non più
    il grande hero. Icona Share **senza sfondo**, icona menu "..." in un
    **cerchio con bordo sottile** (`OutlinedIconButton`) — replica esatta dei
    due stili diversi di icona nello screenshot di riferimento.
  - **Checkbox circolari** (`CircleCheckbox`, non più quadrate): vuota outline
    quando da prendere, piena `onBackground` con spunta `background` quando presa.
  - **Righe piatte**: niente più `Surface`/card/bordo/ombra per articolo; solo
    `Row` + `HorizontalDivider` sottile (`outlineVariant`, indentato dopo la
    checkbox) tra un articolo e l'altro, per la resa "lista pulita" del
    riferimento.
  - **Pillole quantità outline** (`QuantityPillOutline`): bordo sottile, niente
    riempimento colorato, come i badge orario "3:30 PM" nel riferimento.
  - **Bottoni sotto → barra "quick add" fissa**: eliminati i due pulsanti pieni
    ("Aggiungi manualmente"/"Aggiungi da WhatsApp"). Ora `BottomQuickAddBar`:
    icona clipboard (senza sfondo) a sinistra per l'import WhatsApp + un
    **campo di testo sempre pronto** in stile pillola (`surfaceVariant`,
    arrotondato) a destra, sempre visibile (anche a lista vuota, essendo nello
    `Scaffold.bottomBar`) — replica il pattern "home icon + pill input" del
    riferimento, mappando le due azioni esistenti senza perderle. Il testo
    digitato passa comunque nel parser (`onAddItem` → `addItemsFromText`), quindi
    "2 mele" continua a dare quantità 2 anche da qui.
  - **"+" per sezione**: la sezione "Da prendere" ha un piccolo "+" a destra
    dell'header che porta il focus (tastiera) sul campo di aggiunta rapida in
    basso, via `FocusRequester` condiviso — echeggia il "+" per sezione dello
    screenshot senza duplicare l'input.
  - Rimossi: `InlineAddRow` (pop-up/riga inline separata, sostituita dalla barra
    fissa), `SquareCheckbox`/`QuantityBadge` (rinominati/ristilizzati).
  - `AddFromTextScreen`: stessa coerenza — checkbox placeholder ora cerchio,
    `PreviewCard` da card bordata a riga piatta + divider.
  - **Fix di usabilità post-feedback**: placeholder "Scrivi un articolo…" →
    "Aggiungi al carrello" (più chiaro); riaggiunto lo **stepper quantità**
    dentro la pillola di `BottomQuickAddBar` (appare solo quando il campo non
    è vuoto, per non affollare la barra a riposo); aggiunto **scroll
    automatico** quando `items.size` cresce (`LaunchedEffect(items.size)` con
    confronto al totale precedente) verso l'ultimo elemento di "Da prendere" —
    altrimenti un articolo aggiunto con la tastiera aperta finiva in fondo
    alla lista, nascosto sotto la tastiera, e serviva chiudere la tastiera per
    vederlo.
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
- `1a5b77e` — header piatto: rimossa la "pillola" a gradiente in alto.
- `8d37c6b` — quantità stepper, modifica inline, riordino drag, notifica persistente.
- palette zinc + indigo (attuale) — restyle "premium neutrale" + riordino fluido
  con `Modifier.animateItem()` + toast conferma notifica.

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
