import java.sql.Connection; //conexiune la baza de date
import java.sql.DriverManager; //deschide conexiuni
import java.sql.SQLException;

public class DatabaseUtil {

    private static final String URL =
            "jdbc:sqlite:/home/student/Downloads/SistemeDistribuite/SistemeDistribuite/Lab1/lab1/students.db";
            // calea catre fisierul SQLite
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driverul SQLite nu a fost gasit.", e);
        }
    }

    public static Connection getConnection() throws SQLException { //deschide conexiuni la baza de date SQLite din fisierul students.db
        return DriverManager.getConnection(URL);
    }
}