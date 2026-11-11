# Compilare Folio Home con GitHub Actions (senza Android Studio)

1. Crea un repository GitHub dedicato, per esempio `FOLIO-HOME`, con ramo predefinito `main`. Non confonderlo con `FOLIO-NEWS`.
2. Carica il **contenuto** della cartella `Folio_Home_v0_1_1` alla radice del repository, mantenendo `app/`, `settings.gradle`, `build.gradle`, `gradle.properties` e `.github/workflows/build-apk.yml`. Non creare una cartella extra che racchiuda tutto nel repository.
3. Su macOS la cartella `.github` è normalmente nascosta. In Finder premi **⌘ + Maiusc + .** per mostrarla. Se il caricamento dal browser non preserva la struttura, crea il file `.github/workflows/build-apk.yml` direttamente da GitHub → Add file → Create new file, incollando il contenuto originale dell'archivio.
4. Apri **Actions** → **Folio Home - APK** → **Run workflow** (oppure verifica l'esecuzione automatica dopo il commit).
5. Se la build diventa verde, apri l'esecuzione → **Artifacts** → `FolioHome-v0.1.1-debug`, scarica lo ZIP, estrai `app-debug.apk` e installalo sul dispositivo Android di prova.
6. Dopo l'installazione apri Folio Home → Impostazioni → **Scegli Folio come Home predefinita**; puoi sempre ritornare al launcher originale dalle impostazioni Android.

**Importante:** nel workflow non è presente `android-actions/setup-android@v3` né `chmod +x gradlew`, perché questi passaggi hanno creato errori nel precedente progetto News. Il workflow usa Gradle 8.13 installato su GitHub. Se il runner non possiede Android SDK 36, andrà aggiunto un passaggio di installazione specifico sulla base del log reale.

Non considerare compilata la versione finché GitHub Actions non conferma la build verde.
