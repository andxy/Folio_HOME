# Compilare Folio Home con GitHub Actions (senza Android Studio)

1. Crea un repository GitHub dedicato, per esempio `FOLIO-HOME`, con ramo predefinito `main`. Non confonderlo con `FOLIO-NEWS`.
2. Carica il **contenuto** della cartella `Folio_Home_v0_1` alla radice del repository, mantenendo `app/`, `settings.gradle`, `build.gradle`, `gradle.properties` e `.github/workflows/build-apk.yml`. Non creare una cartella extra che racchiuda tutto nel repository.
3. Su macOS la cartella `.github` è normalmente nascosta. In Finder premi **⌘ + Maiusc + .** per mostrarla. Se il caricamento dal browser non preserva la struttura, crea il file `.github/workflows/build-apk.yml` direttamente da GitHub → Add file → Create new file, incollando il contenuto originale dell'archivio.
4. Apri **Actions** → **Folio Home - APK** → **Run workflow** (oppure verifica l'esecuzione automatica dopo il commit).
5. Se la build diventa verde, apri l'esecuzione → **Artifacts** → `FolioHome-v0.1-debug`, scarica lo ZIP, estrai `app-debug.apk` e installalo sul dispositivo Android di prova.
6. Dopo l'installazione apri Folio Home → Impostazioni → **Scegli Folio come Home predefinita**; puoi sempre ritornare al launcher originale dalle impostazioni Android.

**Ambiente automatico:** GitHub Actions configura Temurin Java 17, Android SDK 36 con build-tools 36.0.0 e Gradle 8.13, quindi esegue `gradle --no-daemon --stacktrace :app:assembleDebug`. In caso di build verde l'APK è disponibile negli Artifacts.

Non considerare compilata la versione finché GitHub Actions non conferma la build verde.
