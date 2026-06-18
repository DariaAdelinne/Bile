package com.sd.laborator.business.models

class Book(private var data: Content) {

    var name: String?
        get() = data.name
        set(value) { data.name = value }

    var author: String?
        get() = data.author
        set(value) { data.author = value }

    var publisher: String?
        get() = data.publisher
        set(value) { data.publisher = value }

    var content: String?
        get() = data.text
        set(value) { data.text = value }

    fun hasAuthor(author: String): Boolean = data.author.equals(author)
    fun hasTitle(title: String): Boolean = data.name.equals(title)
    fun publishedBy(publisher: String): Boolean = data.publisher.equals(publisher)
}
