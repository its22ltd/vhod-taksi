# ОПИС на файловете — проект „Вход Такси" (готово за билд)

Общо: 56 файла. По-долу — всеки файл с кратко описание.

## Готовност за билд (проверка)
- [x] `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties` — Gradle конфигурация
- [x] `gradlew`, `gradlew.bat`, `gradle/wrapper/*` — Gradle wrapper (за компилация)
- [x] `app/build.gradle.kts` — модул на приложението + подписващ ключ
- [x] `app/vhodtaksi.keystore` — постоянен подписващ ключ (обновления без загуба на данни)
- [x] `app/src/main/AndroidManifest.xml` — разрешения и екрани
- [x] `.github/workflows/build-apk.yml` — авто-билд в GitHub Actions
- [x] Целият Kotlin код + ресурси (икони, оформления, стилове)

Билд: качваш папката в GitHub → Actions прави `app-debug.apk`.
(В хранилището с подпапка `vhod-taksi/` работният файл `.github/workflows/build.yml` билдва оттам.)

---

## Приложение — Kotlin код (`app/src/main/java/com/its22/vhodtaksi/`)
| Файл | Описание |
|---|---|
| `Model.kt` | Данни: Апартамент, Разход, Приход + форматиране на суми |
| `Calc.kt` | Сметката: дял на човек и дължимо на апартамент |
| `Prefs.kt` | Настройки и данни **по месеци** (всеки месец със свои данни) |
| `Db.kt` | Локална база (SQLite) с плащанията по месец |
| `Signatures.kt` | Запазване/зареждане на подписите (PNG + Base64 за сървъра) |
| `SignatureView.kt` | Поле за подпис с пръст |
| `Escpos.kt` | Печат като изображение (текст + подпис), ESC/POS растер |
| `BtPrinter.kt` | Bluetooth връзка с 80мм принтера |
| `Receipts.kt` | Съдържание на бележката (с подпис) и справката |
| `Sync.kt` | Синхронизация към MSSQL сървъра (вкл. подписа) |
| `MainActivity.kt` | Каса: месец, списък апартаменти, плащане + подпис |
| `DataActivity.kt` | Данни: визуален редактор (полета и бутони) |
| `ReportActivity.kt` | Справка: кой платил/не, колко и за какво + печат |
| `SettingsActivity.kt` | Настройки: принтер, валута, сървър, тест печат |
| `ApartmentAdapter.kt` | Редовете в Каса |
| `StatusAdapter.kt` | Редовете в Справка |

## Приложение — ресурси (`app/src/main/res/`)
| Файл | Описание |
|---|---|
| `layout/activity_main.xml` | Екран Каса |
| `layout/activity_data.xml` | Екран Данни |
| `layout/activity_report.xml` | Екран Справка |
| `layout/activity_settings.xml` | Екран Настройки |
| `layout/item_apartment.xml` | Ред апартамент/статус |
| `layout/item_history.xml` | Ред за списък (резервен) |
| `values/strings.xml` | Име на приложението |
| `values/colors.xml` | Цветове (тюркоаз) |
| `values/themes.xml` | Тема |
| `mipmap-*/ic_launcher*.png` | Икона на приложението (5 размера, 10 файла) |

## Приложение — конфигурация и билд
| Файл | Описание |
|---|---|
| `app/build.gradle.kts` | Настройки на модула + signingConfig (постоянен ключ) |
| `app/proguard-rules.pro` | Празни правила |
| `app/vhodtaksi.keystore` | Подписващ ключ |
| `app/src/main/AndroidManifest.xml` | Разрешения (Bluetooth, интернет) + екрани |
| `build.gradle.kts` | Root билд файл (версии на плъгини) |
| `settings.gradle.kts` | Хранилища и модули |
| `gradle.properties` | Gradle настройки (AndroidX, UTF-8) |
| `gradle/wrapper/gradle-wrapper.jar` | Gradle wrapper |
| `gradle/wrapper/gradle-wrapper.properties` | Версия на Gradle (8.7) |
| `gradlew`, `gradlew.bat` | Стартери за билд |
| `.github/workflows/build-apk.yml` | Авто-билд на APK |
| `.gitignore` | Игнорирани файлове |

## Сървър (`server/`) — ASP.NET Core + MSSQL
| Файл | Описание |
|---|---|
| `Program.cs` | API: `/api/sync` (приема плащания+подпис), `/api/report`, `/api/signature` |
| `VhodTaksiApi.csproj` | Проект (.NET 8) |
| `appsettings.json` | Връзка към MSSQL + таен токен (попълва се) |
| `schema.sql` | Таблица Payments (+ колона Signature за подписа) |
| `README.md` | Стъпки за пускане на сървъра |

## Документация
| Файл | Описание |
|---|---|
| `README.md` | Как се билдва, инсталира и обновява |
| `ОПИСАНИЕ.md` | Функции, как се смята, история на промените |
| `FILES.md` | Кратък списък с файлове |
| `ОПИС.md` | Този пълен опис |
