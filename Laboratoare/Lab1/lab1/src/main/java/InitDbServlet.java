import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

public class InitDbServlet extends HttpServlet { //mosteneste HttpSevrlet pentru a putea raspunde la request-tui HTTP

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) //pentru cereri HTTP de tin get
            throws ServletException, IOException {

        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS students (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " + //incrementare automata la fiecare inserare
                    "nume TEXT NOT NULL, " +
                    "prenume TEXT NOT NULL, " +
                    "varsta INTEGER NOT NULL" +
                    ")";

            stmt.execute(sql);

            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println("<h3>Baza de date si tabelul students au fost create.</h3>");
            response.getWriter().println("<a href='index.jsp'>Inapoi</a>");

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}