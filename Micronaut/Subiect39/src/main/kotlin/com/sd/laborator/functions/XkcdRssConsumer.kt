package com.sd.laborator.functions

import com.sd.laborator.model.RssEntry
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.springframework.stereotype.Component
import java.util.function.Consumer

/**
 * Functie serverless de tip Consumator (Consumer).
 *
 * Responsabilitate (SRP): primeste XML-ul de la producator, il parseaza
 * si afiseaza la iesirea standard perechile <TITLE, URL>.
 *
 * Parsare:
 *   a) extrage continutul tag-ului <title> din fiecare <entry>
 *   b) extrage atributul href din tag-ul <link href="..."> din fiecare <entry>
 *
 * Principii SOLID:
 *   S - singura responsabilitate: parsare XML + afisare rezultate
 *   O - metoda parseEntries() poate fi suprascrie sau extinsa
 *   D - primeste String de la producator; nu depinde de XkcdRssProducer direct
 */
@Component
class XkcdRssConsumer : Consumer<String> {

    /**
     * Primeste XML-ul Atom, il parseaza si afiseaza perechile <TITLE, URL>.
     * @param xml continutul XML primit de la XkcdRssProducer
     */
    override fun accept(xml: String) {
        println("[XkcdRssConsumer] Procesez XML primit...")

        val entries = parseEntries(xml)

        println("\n" + "=".repeat(70))
        println("XKCD RSS — ${entries.size} intrari gasite")
        println("=".repeat(70))

        entries.forEachIndexed { index, entry ->
            println("${index + 1}. $entry")
        }

        println("=".repeat(70))
    }

    /**
     * Parseaza fluxul Atom XML si extrage perechile (title, url).
     *
     * Structura Atom:
     *   <feed>
     *     <entry>
     *       <title>...</title>
     *       <link href="https://xkcd.com/NNN/" .../>
     *     </entry>
     *   </feed>
     */
    private fun parseEntries(xml: String): List<RssEntry> {
        // Jsoup parseaza XML cu Parser.xmlParser() care respecta case-sensitivity
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())

        return doc.select("entry").map { entry ->
            // a) Preia continutul tag-ului <title>
            val title = entry.selectFirst("title")?.text()?.trim() ?: "(fara titlu)"

            // b) Preia atributul href din <link href="...">
            //    In Atom, <link> are atribut href, nu text
            val url = entry.selectFirst("link[href]")?.attr("href")?.trim() ?: "(fara URL)"

            RssEntry(title = title, url = url)
        }
    }
}
