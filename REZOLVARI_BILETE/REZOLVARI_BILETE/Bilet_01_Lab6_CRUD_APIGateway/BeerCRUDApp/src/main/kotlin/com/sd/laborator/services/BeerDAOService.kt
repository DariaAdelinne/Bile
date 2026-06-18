package com.sd.laborator.services

import com.sd.laborator.interfaces.BeerDAO
import com.sd.laborator.model.Beer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.sql.SQLException
import java.util.regex.Pattern
import javax.annotation.PostConstruct

// RowMapper pentru conversia rezultatelor SQL -> Beer
class BeerRowMapper : RowMapper<Beer?> {
    @Throws(SQLException::class)
    override fun mapRow(rs: ResultSet, rowNum: Int): Beer {
        return Beer(rs.getInt("id"), rs.getString("name"), rs.getFloat("price"))
    }
}

// Serviciu care implementeaza BeerDAO - respecta principiul D din SOLID
// (depinde de abstractizare, nu de implementare concreta)
@Service
class BeerDAOService : BeerDAO {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    // Protejeaza impotriva SQL Injection
    private val pattern: Pattern = Pattern.compile("\\W")

    // Creeaza tabela la pornirea aplicatiei (daca nu exista deja)
    @PostConstruct
    fun init() {
        createBeerTable()
        println("[BeerDAOService] Tabela 'beers' verificata/creata.")
    }

    override fun createBeerTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS beers(
                id    INTEGER PRIMARY KEY AUTOINCREMENT,
                name  VARCHAR(100) UNIQUE,
                price FLOAT
            )
        """.trimIndent())
    }

    override fun addBeer(beer: Beer) {
        if (pattern.matcher(beer.beerName).find()) {
            println("[BeerDAOService] SQL Injection detectat la addBeer: ${beer.beerName}")
            return
        }
        jdbcTemplate.update("INSERT INTO beers(name, price) VALUES (?, ?)", beer.beerName, beer.beerPrice)
    }

    override fun getBeers(): String {
        val result: MutableList<Beer?> = jdbcTemplate.query("SELECT * FROM beers", BeerRowMapper())
        return result.joinToString(separator = "\n")
    }

    override fun getBeerByName(name: String): String? {
        if (pattern.matcher(name).find()) {
            println("[BeerDAOService] SQL Injection detectat la getBeerByName: $name")
            return null
        }
        val result: Beer? = jdbcTemplate.queryForObject(
            "SELECT * FROM beers WHERE name = '$name'", BeerRowMapper()
        )
        return result?.toString()
    }

    override fun getBeerByPrice(price: Float): String {
        val result: MutableList<Beer?> = jdbcTemplate.query(
            "SELECT * FROM beers WHERE price <= $price", BeerRowMapper()
        )
        return result.joinToString(separator = "\n")
    }

    override fun updateBeer(beer: Beer) {
        if (pattern.matcher(beer.beerName).find()) {
            println("[BeerDAOService] SQL Injection detectat la updateBeer: ${beer.beerName}")
            return
        }
        jdbcTemplate.update(
            "UPDATE beers SET name = ?, price = ? WHERE id = ?",
            beer.beerName, beer.beerPrice, beer.beerID
        )
    }

    override fun deleteBeer(name: String) {
        if (pattern.matcher(name).find()) {
            println("[BeerDAOService] SQL Injection detectat la deleteBeer: $name")
            return
        }
        jdbcTemplate.update("DELETE FROM beers WHERE name = ?", name)
    }
}
