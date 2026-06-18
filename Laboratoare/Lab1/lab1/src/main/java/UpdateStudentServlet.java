import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class UpdateStudentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int id = Integer.parseInt(request.getParameter("id"));
        String nume = request.getParameter("nume");
        String prenume = request.getParameter("prenume");
        int varsta = Integer.parseInt(request.getParameter("varsta"));

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE students SET nume=?, prenume=?, varsta=? WHERE id=?")) {

            ps.setString(1, nume);
            ps.setString(2, prenume);
            ps.setInt(3, varsta);
            ps.setInt(4, id);
            ps.executeUpdate();

            response.sendRedirect("list-students");

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}