import beans.StudentBean;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SearchStudentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String q = request.getParameter("q");
        List<StudentBean> students = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM students WHERE nume LIKE ? OR prenume LIKE ?")) {

            ps.setString(1, "%" + q + "%");
            ps.setString(2, "%" + q + "%");

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                StudentBean s = new StudentBean();
                s.setId(rs.getInt("id"));
                s.setNume(rs.getString("nume"));
                s.setPrenume(rs.getString("prenume"));
                s.setVarsta(rs.getInt("varsta"));
                students.add(s);
            }

            request.setAttribute("students", students);
            request.getRequestDispatcher("list-students.jsp").forward(request, response);

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}