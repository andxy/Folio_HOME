# Folio Home v0.1 — launcher Android nativo (progetto sorgente)

Questa è una **prima implementazione reale in Java/Android**, non un mockup HTML, e non una ROM AOSP. L'APK **non è stato compilato in questo ambiente**: il workflow GitHub Actions contenuto nell'archivio deve ancora essere eseguito e i test su telefono devono ancora essere effettuati.

## Funzioni incluse nel codice
- Dichiara il ruolo `HOME` e può essere scelto come launcher predefinito.
- Elenco delle app Android realmente installate e ricerca per nome, senza icone finte.
- Home con icone, apertura app, aggiunta/rimozione dalla Home tramite pressione prolungata.
- Cartelle: creazione, aggiunta/rimozione app, rinomina, eliminazione; salvataggio persistente.
- **Aspetto icone**: originale, originale con forma Folio (predefinita), monocromatica dove disponibile; fallback colorato coerente quando non esiste un'icona monocromatica adattiva.
- Widget Android tramite `AppWidgetHost` e picker di sistema; per questa versione si visualizza **un widget alla volta**. I widget e le relative schermate di configurazione dipendono dalle app e dal dispositivo.
- Accesso a Folio News se installato (`org.folio.news`), browser predefinito, selettore documenti Android.
- Tema chiaro/scuro Folio, pulsanti da almeno 48 dp, drawer, gesto orizzontale verso News e verso le app, gesto dal basso verso l'alto per l'elenco app.
- Scelta launcher predefinito tramite `RoleManager`, dove supportato.

## Limiti dichiarati
- **Non sostituisce SystemUI**: Folio Quiet, pannello rapido, blocco schermo, schermate di sistema e protezione privacy richiedono integrazioni separate in AOSP. Uno swipe dall'area di stato continua a usare la tendina nativa di Android.
- Il collegamento «File» apre il **selettore documenti Android**, non un file manager proprietario Folio; un'app di gestione file completa resta eventuale funzione successiva.
- Il contenitore Folio conserva colori/loghi delle app, ma il ritaglio e la resa di icone legacy/adattive devono essere verificati su smartphone reale.
- Nella modalità monocromatica, se manca l'icona monocromatica adattiva è mantenuto il colore originale, senza inventare loghi.
- La Home è una base nativa essenziale, non riproduce ancora tutti i dettagli estetici del prototipo v0.21 (dock configurabile, pagine multiple, spostamento drag & drop, editor widget multipli ecc.).
- I widget in fullscreen o con configurazioni proprietarie potrebbero richiedere modifiche dopo il primo test.
- Il sistema non chiede permessi speciali di accesso a notifiche o impostazioni.

## Build
Consulta `BUILD_GITHUB.md`. La cartella `.github` è nascosta sui sistemi macOS: nel Finder `⌘ + Maiusc + .` la mostra.

## Pacchetto e versioni
Questo modulo si integra nel repository unico Folio OS; è anche compilabile autonomamente per provare la Home gratuitamente sul telefono di riserva.
