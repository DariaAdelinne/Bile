package ejb;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

// Clasa marcata cu @Entity devine tabel in baza de date prin JPA
@Entity
public class StudentEntity {

    // Cheia primara a studentului
    @Id
    @GeneratedValue
    private int id;

    // Campuri simple care vor fi coloane in tabela
    private String nume;
    private String prenume;
    private int varsta;
    private double medie;

    // Constructor gol obligatoriu pentru JPA
    public StudentEntity() {
    }

    // Getter si setter pentru id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Getter si setter pentru nume
    public String getNume() {
        return nume;
    }

    public void setNume(String nume) {
        this.nume = nume;
    }

    // Getter si setter pentru prenume
    public String getPrenume() {
        return prenume;
    }

    public void setPrenume(String prenume) {
        this.prenume = prenume;
    }

    // Getter si setter pentru varsta
    public int getVarsta() {
        return varsta;
    }

    public void setVarsta(int varsta) {
        this.varsta = varsta;
    }

    // Getter si setter pentru medie
    public double getMedie() {
        return medie;
    }

    public void setMedie(double medie) {
        this.medie = medie;
    }
}