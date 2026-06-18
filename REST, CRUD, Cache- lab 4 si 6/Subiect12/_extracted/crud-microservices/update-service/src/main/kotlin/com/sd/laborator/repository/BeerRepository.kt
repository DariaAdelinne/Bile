package com.sd.laborator.repository

import com.sd.laborator.models.Beer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/**
 * Repository comun pentru accesul la baza de date SQLite.
 * Respecta SRP: se ocupa DOAR de persistenta datelor.
 * Respecta DIP: serviciile depind de interfata IBeerRepository, nu de aceasta clasa.
 */
@Repository
class BeerRepository : IBeerRepository {

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    private val rowMapper = RowMapper<Beer> { rs: ResultSet, _ ->
        Beer(rs.getInt("id"), rs.getString("name"), rs.getFloat("price"))
    }

    override fun initTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS beers(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name VARCHAR(100) UNIQUE,
                price FLOAT)
        """.trimIndent())
    }

    override fun add(beer: Beer) {
        jdbc.update("INSERT INTO beers(name, price) VALUES (?, ?)", beer.name, beer.price)
    }

    override fun getAll(): List<Beer> = jdbc.query("SELECT * FROM beers", rowMapper)

    override fun getById(id: Int): Beer? =
        jdbc.query("SELECT * FROM beers WHERE id = ?", rowMapper, id).firstOrNull()

    override fun getByName(name: String): Beer? =
        jdbc.query("SELECT * FROM beers WHERE name = ?", rowMapper, name).firstOrNull()

    override fun update(beer: Beer) {
        jdbc.update("UPDATE beers SET name = ?, price = ? WHERE id = ?",
            beer.name, beer.price, beer.id)
    }

    override fun delete(id: Int) {
        jdbc.update("DELETE FROM beers WHERE id = ?", id)
    }
}
