package com.sd.laborator.presentation.controllers

import com.sd.laborator.business.interfaces.IFileCacheService
import com.sd.laborator.business.interfaces.ILibraryDAOService
import com.sd.laborator.business.interfaces.ILibraryPrinterService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

// Orchestreaza cererea: verifica mai intai cache-ul pe disc,
// daca nu gaseste un rezultat valid delega la serviciile de business.
@Controller
class LibraryPrinterController {

    @Autowired
    private lateinit var libraryDAOService: ILibraryDAOService

    @Autowired
    private lateinit var libraryPrinterService: ILibraryPrinterService

    @Autowired
    private lateinit var fileCacheService: IFileCacheService

    @RequestMapping("/print", method = [RequestMethod.GET])
    @ResponseBody
    fun customPrint(
        @RequestParam(required = true, name = "format", defaultValue = "") format: String
    ): String {
        val key = "print_$format"

        val cached = fileCacheService.get(key)
        if (cached != null) return cached

        val result = when (format) {
            "html" -> libraryPrinterService.printHTML(libraryDAOService.getBooks())
            "json" -> libraryPrinterService.printJSON(libraryDAOService.getBooks())
            "raw"  -> libraryPrinterService.printRaw(libraryDAOService.getBooks())
            else   -> "Format necunoscut: $format"
        }

        fileCacheService.put(key, result)
        println("[CACHE MISS] key='$key' -> raspuns calculat si stocat in cache")
        return result
    }

    @RequestMapping("/find", method = [RequestMethod.GET])
    @ResponseBody
    fun customFind(
        @RequestParam(required = false, name = "author",    defaultValue = "") author: String,
        @RequestParam(required = false, name = "title",     defaultValue = "") title: String,
        @RequestParam(required = false, name = "publisher", defaultValue = "") publisher: String
    ): String {
        val (key, books) = when {
            author.isNotEmpty()    -> Pair("find_author_$author",       libraryDAOService.findAllByAuthor(author))
            title.isNotEmpty()     -> Pair("find_title_$title",         libraryDAOService.findAllByTitle(title))
            publisher.isNotEmpty() -> Pair("find_publisher_$publisher", libraryDAOService.findAllByPublisher(publisher))
            else -> return "Parametru necunoscut. Folositi: author, title sau publisher."
        }

        val cached = fileCacheService.get(key)
        if (cached != null) return cached

        val result = libraryPrinterService.printJSON(books)
        fileCacheService.put(key, result)
        println("[CACHE MISS] key='$key' -> raspuns calculat si stocat in cache")
        return result
    }
}
