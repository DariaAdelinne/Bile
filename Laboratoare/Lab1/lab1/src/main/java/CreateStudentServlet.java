import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement; //mai sigur decat conectare cu string-uri

public class CreateStudentServlet extends HttpServlet { //mosteneste HttpServer pentru a putea raspunde la request uri

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String nume = request.getParameter("nume");
        String prenume = request.getParameter("prenume");
        int varsta = Integer.parseInt(request.getParameter("varsta"));

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO students(nume, prenume, varsta) VALUES (?, ?, ?)")) {

            ps.setString(1, nume);
            ps.setString(2, prenume);
            ps.setInt(3, varsta);
            ps.executeUpdate();

            response.sendRedirect("list-students");

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}