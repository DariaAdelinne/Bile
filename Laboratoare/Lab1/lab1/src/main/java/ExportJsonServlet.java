import beans.StudentBean;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ExportJsonServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        List<StudentBean> students = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM students");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                StudentBean s = new StudentBean();
                s.setId(rs.getInt("id"));
                s.setNume(rs.getString("nume"));
                s.setPrenume(rs.getString("prenume"));
                s.setVarsta(rs.getInt("varsta"));
                students.add(s);
            }

            //facem din lista de StudentBean -> JSON
            response.setContentType("application/json;charset=UTF-8");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getWriter(), students);

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}