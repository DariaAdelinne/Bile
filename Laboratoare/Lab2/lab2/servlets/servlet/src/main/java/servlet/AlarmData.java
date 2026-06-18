package servlet;

// Clasa simpla folosita ca sa pastram ultimul mesaj de alarma
// Variabila este statica, deci poate fi citita din orice servlet sau thread
public class AlarmData {

    private static String mesaj = "Nu exista alarme.";

    // Seteaza mesajul curent de alarma
    public static synchronized void setMesaj(String mesajNou) {
        mesaj = mesajNou;
    }

    // Returneaza mesajul curent de alarma
    public static synchronized String getMesaj() {
        return mesaj;
    }
}