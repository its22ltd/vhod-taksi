# Сървър (ASP.NET Core + MSSQL)

Малка програма (API), която приема бележките от телефона и ги записва в MSSQL.

## Какво съдържа
- `Program.cs` — API-то (endpoint-и `/api/sync` и `/api/report`)
- `VhodTaksiApi.csproj` — проектът (.NET 8)
- `appsettings.json` — настройки: връзка към MSSQL + таен токен
- `schema.sql` — таблицата в MSSQL

## Стъпки за пускане

1. Инсталирай **.NET 8 SDK** на сървъра (Windows или Linux): https://dotnet.microsoft.com/download
2. Създай базата и таблицата: изпълни `schema.sql` в MSSQL (напр. през SQL Server Management Studio).
3. Отвори `appsettings.json` и попълни:
   - `ConnectionStrings:Mssql` — адрес, база, потребител и парола за MSSQL
   - `ApiToken` — измисли таен ключ (същия въвеждаш и в приложението → Настройки → Токен)
4. В папката `server` изпълни:
   ```
   dotnet run
   ```
   По подразбиране слуша на `http://localhost:5000`.
5. Провери в браузър: отвори адреса — трябва да пише `Vhod Taksi API - OK`.

## Пускане към интернет (за да го достига телефонът)
- **Windows/IIS:** публикувай с `dotnet publish -c Release` и хостни зад IIS. Пусни HTTPS.
- **Linux:** пусни зад Nginx reverse proxy с HTTPS (Let's Encrypt).
- В приложението задай адреса на `/api/sync`, напр. `https://tvoia-server.bg/api/sync`.

## Отчет от базата
Отвори в браузър:
```
https://tvoia-server.bg/api/report?token=ТВОЯ_ТОКЕН&period=м.04.2026
```
Връща брой платили и обща събрана сума за периода.
