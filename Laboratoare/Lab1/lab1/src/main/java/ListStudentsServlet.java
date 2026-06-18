import beans.StudentBean;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ListStudentsServlet extends HttpServlet { //citeste studentii din baza de date si i trimite catre json

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        List<StudentBean> students = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM students");
             ResultSet rs = ps.executeQuery()) { //rezultatul tabelar al select-ului

            while (rs.next()) { //cat timp mai exista randuri pe care sa se mute cursorul
                StudentBean s = new StudentBean(); //creez un obiect nou cu valorile din randul curent si le pun in el
                s.setId(rs.getInt("id"));
                s.setNume(rs.getString("nume"));
                s.setPrenume(rs.getString("prenume"));
                s.setVarsta(rs.getInt("varsta"));
                students.add(s);
            }

            request.setAttribute("students", students); //atasez lista de studenti la request
            request.getRequestDispatcher("list-students.jsp").forward(request, response); //forward catre pagina JSP care va afisa lista

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}