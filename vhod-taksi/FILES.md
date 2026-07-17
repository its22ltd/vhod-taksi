# Списък с файловете в проекта

## Приложение (Android, Kotlin)
Логика и данни:
- `Model.kt` — данни: Апартамент, Разход, Приход + форматиране
- `Calc.kt` — сметката (дял на човек, дължимо на апартамент)
- `Prefs.kt` — настройки + данни по месеци (всеки месец със свои данни)
- `Db.kt` — локална база (SQLite) с плащанията (по месец)
- `Sync.kt` — синхронизация към MSSQL сървъра
- `Escpos.kt` — печат като изображение (кирилица на всеки принтер)
- `BtPrinter.kt` — Bluetooth връзка с принтера
- `Receipts.kt` — съдържание на бележката и справката

Екрани:
- `MainActivity.kt` — Каса: избор на месец, „Генерирай нов месец", списък апартаменти
- `DataActivity.kt` — Данни: визуален редактор (полета и бутони)
- `ReportActivity.kt` — Справка: кой е платил/не, колко и за какво + печат
- `SettingsActivity.kt` — Настройки (принтер, сървър, валута)
- `ApartmentAdapter.kt` — редовете в Каса
- `StatusAdapter.kt` — редовете в Справка

Ресурси и билд:
- `app/src/main/res/layout/*.xml` — екрани
- `app/src/main/res/values/` — strings, colors, themes
- `app/src/main/res/mipmap-*/` — икона
- `app/src/main/AndroidManifest.xml` — разрешения и екрани
- `app/vhodtaksi.keystore` — постоянен подписващ ключ (за обновления без загуба на данни)
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`
- `gradlew`, `gradle/wrapper/*` — за компилация
- `.github/workflows/build-apk.yml` — авто-билд на APK в GitHub
  (виж и `build.yml` в корена на хранилището, който билдва от подпапката)

## Сървър (ASP.NET Core + MSSQL)
- `server/Program.cs` — API-то (`/api/sync`, `/api/report`)
- `server/VhodTaksiApi.csproj` — проектът (.NET 8)
- `server/appsettings.json` — връзка към MSSQL + токен (попълва се)
- `server/schema.sql` — таблицата в MSSQL
- `server/README.md` — стъпки за пускане на сървъра

## Документация
- `README.md` — как се билдва, инсталира и ползва
- `ОПИСАНИЕ.md` — какво прави приложението, функции, как се смята, история на промените
- `FILES.md` — този списък
