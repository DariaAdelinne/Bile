package com.sd.laborator

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * ErrorReportController - serviciul REST propriu-zis (cerinta biletului).
 *
 *   POST /errors      - primeste o eroare de la procesorul de flux si o adauga in errors.xml
 *   GET  /            - afiseaza in browser pagina completa (HTML) cu erorile + statisticile
 *   GET  /errors.xml  - fisierul XML brut (pentru verificare)
 *
 * SOLID(S): responsabilitatea de prezentare (web) e separata de cea de stocare (XmlErrorStore).
 * SOLID(D): controllerul depinde de XmlErrorStore injectat, nu de detaliile de scriere a XML-ului.
 */
@RestController
open class ErrorReportController(private val store: XmlErrorStore) {

    @PostMapping("/errors")
    open fun addError(
        @RequestParam type: String,
        @RequestParam sourcePort: String,
        @RequestParam detail: String
    ): String {
        store.append(type, sourcePort, detail)
        println("[REST] Adaugat in errors.xml: $type (port $sourcePort) - $detail")
        return "OK"
    }

    @GetMapping("/errors.xml", produces = [MediaType.APPLICATION_XML_VALUE])
    open fun xml(): String = store.rawXml()

    @GetMapping("/", produces = [MediaType.TEXT_HTML_VALUE])
    open fun page(): String {
        val errors = store.readAll()
        val byType = errors.groupingBy { it["type"] ?: "?" }.eachCount()
            .toList().sortedByDescending { it.second }

        val sb = StringBuilder()
        sb.append(
            """
            <!DOCTYPE html>
            <html lang="ro">
            <head>
              <meta charset="utf-8"/>
              <meta http-equiv="refresh" content="5"/>
              <title>Raport erori de comunicare TCP</title>
              <style>
                body { font-family: Segoe UI, Arial, sans-serif; margin: 24px; background:#f6f7f9; color:#222; }
                h1 { color:#b00020; }
                table { border-collapse: collapse; width: 100%; background:#fff; margin-bottom:28px; }
                th, td { border: 1px solid #ccc; padding: 8px 10px; text-align: left; }
                th { background:#333; color:#fff; }
                tr:nth-child(even) { background:#f0f0f0; }
                .type { font-weight:bold; }
                .CONNECTION_RESET { color:#b00020; }
                .EOF_UNEXPECTED  { color:#9a6700; }
                .MALFORMED_MESSAGE { color:#6f42c1; }
                .BROKEN_PIPE { color:#0b5394; }
                .empty { color:#388e3c; font-size:1.1em; }
                .muted { color:#777; font-size:0.9em; }
              </style>
            </head>
            <body>
              <h1>Raport erori de comunicare TCP (chat - laborator 8)</h1>
              <p class="muted">Pagina se reimprospateaza automat la 5 secunde. Sursa: <code>errors.xml</code>.
                 Vezi si <a href="/errors.xml">/errors.xml</a>.</p>
            """.trimIndent()
        )

        if (errors.isEmpty()) {
            sb.append("<p class=\"empty\">Nicio eroare de comunicare inregistrata pana acum. ✔</p>")
        } else {
            // tabel de STATISTICI pe tipuri
            sb.append("<h2>Statistici (total: ${errors.size})</h2>")
            sb.append("<table><tr><th>Tip eroare</th><th>Numar aparitii</th></tr>")
            for ((type, count) in byType) {
                sb.append("<tr><td class=\"type $type\">${esc(type)}</td><td>$count</td></tr>")
            }
            sb.append("</table>")

            // tabel DETALIAT cu fiecare eroare
            sb.append("<h2>Detalii</h2>")
            sb.append("<table><tr><th>#</th><th>Timestamp</th><th>Tip</th><th>Port sursa</th><th>Detaliu</th></tr>")
            errors.forEachIndexed { i, e ->
                val t = e["type"] ?: ""
                sb.append(
                    "<tr><td>${i + 1}</td><td>${esc(e["timestamp"])}</td>" +
                        "<td class=\"type $t\">${esc(t)}</td>" +
                        "<td>${esc(e["sourcePort"])}</td><td>${esc(e["detail"])}</td></tr>"
                )
            }
            sb.append("</table>")
        }

        sb.append("</body></html>")
        return sb.toString()
    }

    /** Escapa caracterele speciale HTML pentru a evita stricarea paginii / injectia. */
    private fun esc(s: String?): String =
        (s ?: "")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
