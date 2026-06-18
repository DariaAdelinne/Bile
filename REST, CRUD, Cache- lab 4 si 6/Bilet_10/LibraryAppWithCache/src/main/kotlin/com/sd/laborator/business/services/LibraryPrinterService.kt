package com.sd.laborator.business.services

import com.sd.laborator.business.interfaces.ILibraryPrinterService
import com.sd.laborator.business.models.Book
import org.springframework.stereotype.Service

@Service
class LibraryPrinterService : ILibraryPrinterService {

    override fun printHTML(books: Set<Book>): String {
        var content = "<html><head><title>Libraria mea HTML</title></head><body>"
        books.forEach {
            content += "<p><h3>${it.name}</h3><h4>${it.author}</h4><h5>${it.publisher}</h5>${it.content}</p><br/>"
        }
        content += "</body></html>"
        return content
    }

    override fun printJSON(books: Set<Book>): String {
        var content = "[\n"
        books.forEachIndexed { index, book ->
            val comma = if (index < books.size - 1) "," else ""
            content += "    {\"Titlu\": \"${book.name}\", \"Autor\":\"${book.author}\", \"Editura\":\"${book.publisher}\", \"Text\":\"${book.content}\"}$comma\n"
        }
        content += "]\n"
        return content
    }

    override fun printRaw(books: Set<Book>): String {
        var content = ""
        books.forEach {
            content += "${it.name}\n${it.author}\n${it.publisher}\n${it.content}\n\n"
        }
        return content
    }
}
