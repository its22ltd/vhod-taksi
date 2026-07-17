# Списък с файловете в проекта

## Приложение (Android, Kotlin)
Логика и данни:
- `app/src/main/java/com/its22/vhodtaksi/Model.kt` — данни: Апартамент, Разход, Приход + форматиране
- `app/src/main/java/com/its22/vhodtaksi/Calc.kt` — сметката (дял на човек, дължимо на апартамент)
- `app/src/main/java/com/its22/vhodtaksi/Prefs.kt` — настройки + данни (с твоите примерни стойности)
- `app/src/main/java/com/its22/vhodtaksi/Db.kt` — локална база (SQLite) с плащанията
- `app/src/main/java/com/its22/vhodtaksi/Sync.kt` — синхронизация към сървъра
- `app/src/main/java/com/its22/vhodtaksi/Escpos.kt` — печат като изображение (кирилица на всеки принтер)
- `app/src/main/java/com/its22/vhodtaksi/BtPrinter.kt` — Bluetooth връзка с принтера
- `app/src/main/java/com/its22/vhodtaksi/Receipts.kt` — съдържание на бележката и отчета

Екрани:
- `app/src/main/java/com/its22/vhodtaksi/MainActivity.kt` — Каса: списък апартаменти + събиране/печат
- `app/src/main/java/com/its22/vhodtaksi/DataActivity.kt` — Данни: апартаменти/разходи/приходи + преглед
- `app/src/main/java/com/its22/vhodtaksi/ReportActivity.kt` — Отчет + история + печат
- `app/src/main/java/com/its22/vhodtaksi/SettingsActivity.kt` — Настройки (принтер, сървър, валута)
- `app/src/main/java/com/its22/vhodtaksi/ApartmentAdapter.kt` — редовете с апартаменти
- `app/src/main/java/com/its22/vhodtaksi/PaymentAdapter.kt` — редовете с плащания

Оформления и ресурси:
- `app/src/main/res/layout/*.xml` — екрани (activity_main, activity_data, activity_report, activity_settings, item_apartment, item_history)
- `app/src/main/res/values/` — strings.xml, colors.xml, themes.xml
- `app/src/main/res/mipmap-*/` — икона на приложението
- `app/src/main/AndroidManifest.xml` — разрешения (Bluetooth, интернет) и екрани

Конфигурация на билда:
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- `app/build.gradle.kts`, `app/proguard-rules.pro`
- `gradlew`, `gradlew.bat`, `gradle/wrapper/*` — за компилация
- `.github/workflows/build-apk.yml` — автоматичен билд на APK в GitHub
- `.gitignore`

## Сървър (ASP.NET Core + MSSQL)
- `server/Program.cs` — API-то (`/api/sync`, `/api/report`)
- `server/VhodTaksiApi.csproj` — проектът (.NET 8)
- `server/appsettings.json` — връзка към MSSQL + токен (попълва се)
- `server/schema.sql` — таблицата в MSSQL
- `server/README.md` — стъпки за пускане на сървъра

## Документация
- `README.md` — как се билдва, инсталира и ползва
- `FILES.md` — този списък
