package com.its22.vhodtaksi

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        title = "Настройки"

        val etBiz = findViewById<EditText>(R.id.etBiz)
        val etSub = findViewById<EditText>(R.id.etSub)
        val etFooter = findViewById<EditText>(R.id.etFooter)
        val etCur = findViewById<EditText>(R.id.etCur)
        val etServer = findViewById<EditText>(R.id.etServer)
        val etToken = findViewById<EditText>(R.id.etToken)
        val tvPrinter = findViewById<TextView>(R.id.tvPrinter)
        val tvWidth = findViewById<TextView>(R.id.tvWidth)

        etBiz.setText(Prefs.businessName(this))
        etSub.setText(Prefs.subtitle(this))
        etFooter.setText(Prefs.footer(this))
        etCur.setText(Prefs.currency(this))
        etServer.setText(Prefs.serverUrl(this))
        etToken.setText(Prefs.serverToken(this))
        updatePrinterLabel(tvPrinter)
        updateWidthLabel(tvWidth)
        findViewById<TextView>(R.id.tvVersion).text = "Версия: " + APP_VERSION

        findViewById<MaterialButton>(R.id.btnPickPrinter).setOnClickListener { pickPrinter(tvPrinter) }
        findViewById<MaterialButton>(R.id.btnWidth).setOnClickListener { pickWidth(tvWidth) }
        findViewById<MaterialButton>(R.id.btnTest).setOnClickListener { testPrint() }
        findViewById<MaterialButton>(R.id.btnSync).setOnClickListener { syncNow(etServer, etToken) }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            Prefs.setBusinessName(this, etBiz.text.toString().trim().ifEmpty { "ЕТАЖНА СОБСТВЕНОСТ" })
            Prefs.setSubtitle(this, etSub.text.toString().trim())
            Prefs.setFooter(this, etFooter.text.toString().trim())
            Prefs.setCurrency(this, etCur.text.toString().trim().ifEmpty { "€" })
            Prefs.setServerUrl(this, etServer.text.toString().trim())
            Prefs.setServerToken(this, etToken.text.toString().trim())
            toast("Запазено")
            finish()
        }
    }

    private fun updatePrinterLabel(tv: TextView) {
        val n = Prefs.printerName(this)
        val m = Prefs.printerMac(this)
        tv.text = if (m.isBlank()) "Не е избран принтер"
        else if (n.isBlank()) m else n + "  (" + m + ")"
    }

    private fun updateWidthLabel(tv: TextView) {
        tv.text = if (Prefs.paperDots(this) <= 400) "Текущо: 58 мм" else "Текущо: 80 мм"
    }

    @SuppressLint("MissingPermission")
    private fun pickPrinter(tv: TextView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                2
            )
            toast("Разрешете Bluetooth и опитайте пак")
            return
        }
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            toast("Устройството няма Bluetooth")
            return
        }
        if (!adapter.isEnabled) {
            toast("Включете Bluetooth и опитайте пак")
            return
        }
        val bonded = adapter.bondedDevices.toList()
        if (bonded.isEmpty()) {
            toast("Няма сдвоени устройства. Сдвоете принтера от настройките на телефона (Bluetooth).")
            return
        }
        val names = bonded.map { (it.name ?: "?") + "\n" + it.address }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Изберете принтер")
            .setItems(names) { _, which ->
                val d = bonded[which]
                Prefs.setPrinter(this, d.address, d.name ?: "")
                updatePrinterLabel(tv)
            }
            .show()
    }

    private fun pickWidth(tv: TextView) {
        val opts = arrayOf("58 мм (384 точки)", "80 мм (576 точки)")
        AlertDialog.Builder(this)
            .setTitle("Ширина на хартията")
            .setItems(opts) { _, which ->
                Prefs.setPaperDots(this, if (which == 0) 384 else 576)
                updateWidthLabel(tv)
            }
            .show()
    }

    private fun syncNow(etServer: EditText, etToken: EditText) {
        // запазваме адреса/токена преди синхронизация
        Prefs.setServerUrl(this, etServer.text.toString().trim())
        Prefs.setServerToken(this, etToken.text.toString().trim())
        if (Prefs.serverUrl(this).isBlank()) {
            toast("Въведете адрес на сървъра")
            return
        }
        toast("Синхронизация...")
        Thread {
            try {
                val n = Sync.push(this)
                runOnUiThread { toast("Синхронизирани: " + n + " бележки") }
            } catch (e: Exception) {
                runOnUiThread { toast("Грешка: " + (e.message ?: "")) }
            }
        }.start()
    }

    private fun testPrint() {
        val lines = listOf(
            Escpos.Line("ТЕСТ ПЕЧАТ", size = 40f, bold = true, align = Escpos.Align.CENTER, extra = 12f),
            Escpos.Line(separator = true),
            Escpos.Line("Кирилица: АБВГ абвг", size = 26f),
            Escpos.Line("Цифри: 1234567890", size = 26f),
            Escpos.Line(separator = true),
            Escpos.Line("Ако виждате това - готово!", size = 24f, align = Escpos.Align.CENTER, extra = 10f)
        )
        val dots = Prefs.paperDots(this)
        val mac = Prefs.printerMac(this)
        toast("Печат...")
        Thread {
            try {
                val bmp = Escpos.buildReceiptBitmap(dots, lines)
                BtPrinter.printBytes(mac, Escpos.bitmapToEscPos(bmp))
                runOnUiThread { toast("Готово") }
            } catch (e: Exception) {
                runOnUiThread { toast("Грешка: " + (e.message ?: "")) }
            }
        }.start()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
