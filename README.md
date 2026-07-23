# Lista Spesa da WhatsApp

App Android che trasforma il classico messaggio WhatsApp della spesa — una riga di
testo con virgole, senza ordine né checkbox — in una lista della spesa vera e propria,
da spuntare articolo per articolo mentre sei al supermercato.

Esempio di input (il classico messaggio "fai la spesa"):

```
Acqua bambini, pollo pannato/nuggets, iogurt Danone, bicchieri riso, 2 vaschette
menestra, 3 fanta, pizza, 2 kombucha, torrone al tartufo (prendi tutti i pacchi
che ci siano mi sa che stanno finendo anche la), palla latuga, tinto normale,
2 pere, 2 mele, 2 pacchi tacchino a fette
```

diventa una lista di articoli distinti, ciascuno con la sua quantità e un checkbox,
comprese eventuali note tra parentesi mostrate come sottotitolo dell'articolo.

## Come si usa

Due modi per portare il testo dentro l'app:

1. **Condivisione da WhatsApp**: seleziona il messaggio in WhatsApp, tocca "Condividi"
   (o "Inoltra" → icona di condivisione) e scegli "Lista Spesa da WhatsApp". L'app si
   apre già con il testo incollato e la lista pronta da confermare.
2. **Incolla manualmente**: apri l'app, tocca "Aggiungi dal testo", incolla (o scrivi)
   il messaggio e conferma.

Nella lista puoi spuntare gli articoli man mano che li metti nel carrello (finiscono
in fondo, nella sezione "Nel carrello"), eliminarli con l'icona del cestino, svuotare
gli articoli già spuntati o l'intera lista dal menu in alto. Tutto è salvato in locale
(database Room) e resta disponibile anche chiudendo l'app.

## Come funziona il parsing

Il testo viene interpretato riga per riga e poi diviso in singoli articoli sulle
virgole, con qualche accorgimento pensato apposta per i messaggi WhatsApp reali:

- le virgole **dentro le parentesi** non spezzano l'articolo (es. `torrone al tartufo
  (prendi tutti i pacchi che ci siano...)` resta un unico articolo con nota);
- un numero iniziale diventa la quantità: `2 pere` → *Pere ×2*;
- gli asterischi/underscore/tilde di grassetto, corsivo e barrato di WhatsApp
  (`*bold*`, `_italic_`, `~strike~`) vengono rimossi;
- righe puntate o numerate (`- Latte`, `• Pane`, `1. Uova`) sono riconosciute;
- le intestazioni delle chat esportate (`[20:20, 23/07/2026] Nome: ...` oppure
  `20:20 - Nome: ...`) vengono tolte automaticamente;
- articoli ripetuti nello stesso testo (anche su più messaggi/righe) vengono uniti
  sommando le quantità; se aggiungi altro testo in un secondo momento, un articolo
  già presente e non ancora spuntato viene aggiornato invece di duplicarsi.

Tutta questa logica vive nel modulo `parser` (Kotlin puro, senza dipendenze Android),
con una suite di test in `parser/src/test/kotlin/.../WhatsAppListParserTest.kt` che
verifica proprio l'esempio sopra e i vari casi limite.

## Architettura

- **`parser`** — modulo Kotlin/JVM puro con `WhatsAppListParser`: testo grezzo →
  `List<ParsedItem>` (nome, quantità, nota). Nessuna dipendenza Android, quindi
  testabile con `./gradlew :parser:test` senza bisogno dell'SDK Android.
- **`app`** — app Android in Kotlin + Jetpack Compose (Material 3):
  - `data/` — entità Room (`ShoppingItem`), DAO, database e `ShoppingListRepository`
    (gestisce anche l'unione degli articoli duplicati);
  - `ui/` — `ShoppingListViewModel`, schermata principale (`HomeScreen`) e schermata
    di incolla/anteprima (`AddFromTextScreen`);
  - `MainActivity` — gestisce anche l'intent `ACTION_SEND` con cui WhatsApp condivide
    il testo selezionato.

## Build

Requisiti: JDK 17+, Android SDK (compileSdk 34) — tipicamente basta aprire il
progetto in Android Studio (Koala o più recente) e lasciare che sincronizzi.

Da riga di comando:

```bash
./gradlew assembleDebug   # genera app/build/outputs/apk/debug/app-debug.apk
./gradlew test            # esegue tutti gli unit test (parser + app)
./gradlew :parser:test    # solo i test del parser (non richiedono l'SDK Android)
```

Una GitHub Action (`.github/workflows/android-ci.yml`) esegue automaticamente test e
build a ogni push/PR e pubblica l'APK di debug come artifact.

> Nota: questo progetto è stato scritto in un ambiente sandbox privo di Android SDK,
> quindi il modulo `parser` è stato compilato e testato direttamente, mentre il modulo
> `app` è stato verificato "a occhio" e va controllato con una build reale (Android
> Studio o la CI) prima del primo utilizzo.
