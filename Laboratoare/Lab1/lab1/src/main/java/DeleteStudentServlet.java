import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class DeleteStudentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int id = Integer.parseInt(request.getParameter("id"));

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM students WHERE id=?")) {

            ps.setInt(1, id);
            ps.executeUpdate();

            response.sendRedirect("list-students");

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}