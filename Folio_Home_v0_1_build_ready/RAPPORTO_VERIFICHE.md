# Folio Home v0.1 — rapporto di verifica

## Risultato
**APK non prodotto in questo ambiente.** L'ambiente locale dispone di OpenJDK 21, ma non di Java 17, Android SDK o Gradle. L'accesso a Internet per installare i componenti è disabilitato. Il comando `:app:assembleDebug` non è quindi eseguibile qui e non viene dichiarato riuscito.

## Verifiche eseguite
- Archivio ZIP originale: letto correttamente (12 file).
- File XML Android: parsing di 3 file riuscito.
- Percorsi essenziali Gradle, manifest e activity: presenti.
- Sorgente Java: controllo preliminare con `javac --release 17`; non verificabile fino in fondo perché mancano le classi `android.*` dell'SDK. Le segnalazioni di simboli mancanti non dimostrano errori nel sorgente.
- Test unitari: non presenti nel progetto consegnato; non eseguiti.
- Test strumentali e su dispositivo: non eseguiti.
- `:app:assembleDebug`: **non eseguito** (Gradle assente); nessun APK disponibile.

## Modifiche
- Aggiornato `.github/workflows/build-apk.yml` affinché GitHub Actions installi esplicitamente Java 17, Android SDK platform 36 / build-tools 36.0.0 e Gradle 8.13 prima di lanciare `:app:assembleDebug`.
- Aggiornato `BUILD_GITHUB.md`.
- Nessuna modifica a funzionalità, UI, manifest o file Java.

## Compilazione
Caricare il contenuto della directory alla radice di un repository GitHub, aprire **Actions → Folio Home - APK → Run workflow** e, se la build termina con successo, scaricare l'artifact `FolioHome-v0.1-debug` contenente `app-debug.apk`. L'esito finale deve essere verificato nel log della run.
