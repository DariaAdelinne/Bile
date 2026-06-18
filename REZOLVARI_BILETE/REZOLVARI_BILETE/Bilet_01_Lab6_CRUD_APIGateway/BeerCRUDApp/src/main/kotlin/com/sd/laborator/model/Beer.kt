package com.sd.laborator.model

class Beer(private var id: Int, private var name: String, private var price: Float) {

    var beerID: Int
        get() = id
        set(value) { id = value }

    var beerName: String
        get() = name
        set(value) { name = value }

    var beerPrice: Float
        get() = price
        set(value) { price = value }

    override fun toString(): String {
        return "Beer [id=$beerID, name=$beerName, price=$beerPrice]"
    }
}
