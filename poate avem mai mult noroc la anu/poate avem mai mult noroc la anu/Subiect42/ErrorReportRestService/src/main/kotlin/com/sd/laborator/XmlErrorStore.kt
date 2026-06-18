package com.sd.laborator

import org.springframework.stereotype.Component
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * XmlErrorStore - persistenta locala a erorilor de comunicare intr-un fisier XML (errors.xml).
 *
 * Structura fisierului:
 *   <errorReport>
 *     <error timestamp="..." type="CONNECTION_RESET" sourcePort="6003" detail="..."/>
 *     ...
 *   </errorReport>
 *
 * SOLID(S): singura responsabilitate = citirea/scrierea raportului de erori in XML
 *           (separat de logica web din controller).
 */
@Component
open class XmlErrorStore {
    private val file = File("errors.xml")
    private val tsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /** Adauga o eroare noua in fisierul XML (sincronizat, ca apelurile concurente sa nu se calce). */
    @Synchronized
    open fun append(type: String, sourcePort: String, detail: String) {
        val doc = load()
        val el = doc.createElement("error")
        el.setAttribute("timestamp", LocalDateTime.now().format(tsFormat))
        el.setAttribute("type", type)
        el.setAttribute("sourcePort", sourcePort)
        el.setAttribute("detail", detail)
        doc.documentElement.appendChild(el)
        save(doc)
    }

    /** Citeste toate erorile din XML, ca lista de map-uri (cheie -> valoare). */
    @Synchronized
    open fun readAll(): List<Map<String, String>> {
        if (!file.exists()) return emptyList()
        val doc = load()
        val nodes = doc.getElementsByTagName("error")
        val out = ArrayList<Map<String, String>>()
        for (i in 0 until nodes.length) {
            val e = nodes.item(i) as Element
            out.add(
                mapOf(
                    "timestamp" to e.getAttribute("timestamp"),
                    "type" to e.getAttribute("type"),
                    "sourcePort" to e.getAttribute("sourcePort"),
                    "detail" to e.getAttribute("detail")
                )
            )
        }
        return out
    }

    /** Continutul XML brut (pentru endpoint-ul de verificare /errors.xml). */
    @Synchronized
    open fun rawXml(): String = if (file.exists()) file.readText() else "<errorReport/>"

    private fun load(): Document {
        val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        return if (file.exists() && file.length() > 0) {
            db.parse(file)
        } else {
            val doc = db.newDocument()
            doc.appendChild(doc.createElement("errorReport"))
            doc
        }
    }

    private fun save(doc: Document) {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.transform(DOMSource(doc), StreamResult(file))
    }
}
