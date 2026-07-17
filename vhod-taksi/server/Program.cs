using Microsoft.Data.SqlClient;
using System.Text.Json;

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

string ConnStr() =>
    app.Configuration.GetConnectionString("Mssql")
    ?? "Server=localhost;Database=VhodTaksi;Trusted_Connection=True;TrustServerCertificate=True";

string ApiToken() => app.Configuration["ApiToken"] ?? "SMENI_TOZI_TOKEN";

// Проверка, че работи
app.MapGet("/", () => "Vhod Taksi API - OK");

// Приемане на бележки от телефона -> запис в MSSQL
app.MapPost("/api/sync", async (HttpRequest req) =>
{
    using var reader = new StreamReader(req.Body);
    var raw = await reader.ReadToEndAsync();

    JsonDocument doc;
    try { doc = JsonDocument.Parse(raw); }
    catch { return Results.Json(new { ok = false, error = "invalid json" }); }

    var root = doc.RootElement;
    var token = root.TryGetProperty("token", out var t) ? t.GetString() : "";
    if (token != ApiToken())
        return Results.Json(new { ok = false, error = "bad token" }, statusCode: 401);

    var device = root.TryGetProperty("device", out var d) ? (d.GetString() ?? "unknown") : "unknown";

    if (!root.TryGetProperty("records", out var recs) || recs.ValueKind != JsonValueKind.Array)
        return Results.Json(new { ok = false, error = "no records" });

    int saved = 0;
    try
    {
        await using var conn = new SqlConnection(ConnStr());
        await conn.OpenAsync();

        foreach (var r in recs.EnumerateArray())
        {
            var uuid = GetStr(r, "uuid");
            if (string.IsNullOrEmpty(uuid)) continue;

            var cmd = conn.CreateCommand();
            cmd.CommandText = @"
MERGE Payments AS tgt
USING (SELECT @Uuid AS Uuid) AS src ON tgt.Uuid = src.Uuid
WHEN MATCHED THEN UPDATE SET
    Device=@Device, Apt=@Apt, Name=@Name, People=@People, Period=@Period,
    Amount=@Amount, Personal=@Personal, ElevatorShare=@Elevator, OtherShare=@Other, Ts=@Ts
WHEN NOT MATCHED THEN INSERT
    (Uuid, Device, Apt, Name, People, Period, Amount, Personal, ElevatorShare, OtherShare, Ts)
    VALUES (@Uuid, @Device, @Apt, @Name, @People, @Period, @Amount, @Personal, @Elevator, @Other, @Ts);";

            cmd.Parameters.AddWithValue("@Uuid", uuid);
            cmd.Parameters.AddWithValue("@Device", device);
            cmd.Parameters.AddWithValue("@Apt", GetStr(r, "apt"));
            cmd.Parameters.AddWithValue("@Name", (object?)GetStr(r, "name") ?? DBNull.Value);
            cmd.Parameters.AddWithValue("@People", GetInt(r, "people"));
            cmd.Parameters.AddWithValue("@Period", GetStr(r, "period"));
            cmd.Parameters.AddWithValue("@Amount", GetDec(r, "amount"));
            cmd.Parameters.AddWithValue("@Personal", GetDec(r, "personal"));
            cmd.Parameters.AddWithValue("@Elevator", GetDec(r, "elevatorShare"));
            cmd.Parameters.AddWithValue("@Other", GetDec(r, "otherShare"));
            cmd.Parameters.AddWithValue("@Ts", GetLong(r, "ts"));

            await cmd.ExecuteNonQueryAsync();
            saved++;
        }
    }
    catch (Exception ex)
    {
        return Results.Json(new { ok = false, error = ex.Message }, statusCode: 500);
    }

    return Results.Json(new { ok = true, saved });
});

// Кратък отчет по период: /api/report?token=...&period=м.04.2026
app.MapGet("/api/report", async (HttpRequest req) =>
{
    var token = req.Query["token"].ToString();
    if (token != ApiToken())
        return Results.Json(new { ok = false, error = "bad token" }, statusCode: 401);

    var period = req.Query["period"].ToString();

    await using var conn = new SqlConnection(ConnStr());
    await conn.OpenAsync();
    var cmd = conn.CreateCommand();
    if (string.IsNullOrEmpty(period))
    {
        cmd.CommandText = "SELECT COUNT(*), COALESCE(SUM(Amount),0) FROM Payments";
    }
    else
    {
        cmd.CommandText = "SELECT COUNT(*), COALESCE(SUM(Amount),0) FROM Payments WHERE Period=@p";
        cmd.Parameters.AddWithValue("@p", period);
    }

    await using var rd = await cmd.ExecuteReaderAsync();
    int count = 0;
    decimal total = 0m;
    if (await rd.ReadAsync())
    {
        count = rd.GetInt32(0);
        total = rd.GetDecimal(1);
    }
    return Results.Json(new { ok = true, period, count, total });
});

app.Run();

// --- помощни функции за четене на JSON полета ---
static string GetStr(JsonElement e, string n) =>
    e.TryGetProperty(n, out var v) && v.ValueKind == JsonValueKind.String ? (v.GetString() ?? "") : "";

static int GetInt(JsonElement e, string n) =>
    e.TryGetProperty(n, out var v) && v.ValueKind == JsonValueKind.Number && v.TryGetInt32(out var i) ? i : 0;

static long GetLong(JsonElement e, string n) =>
    e.TryGetProperty(n, out var v) && v.ValueKind == JsonValueKind.Number && v.TryGetInt64(out var i) ? i : 0L;

static decimal GetDec(JsonElement e, string n) =>
    e.TryGetProperty(n, out var v) && v.ValueKind == JsonValueKind.Number && v.TryGetDecimal(out var i) ? i : 0m;
