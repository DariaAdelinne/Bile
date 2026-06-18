import beans.StudentBean;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class ReadStudentServlet extends HttpServlet {

    private static final String FILE_PATH =
            "/Users/daria.adelinne/Downloads/glassfish5/student.xml";

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        File file = new File(FILE_PATH);

        if (!file.exists()) {
            response.sendError(404, "Nu a fost gasit niciun student serializat pe disc!");
            return;
        }

        XmlMapper xmlMapper = new XmlMapper();
        StudentBean bean = xmlMapper.readValue(file, StudentBean.class);

        request.setAttribute("nume", bean.getNume());
        request.setAttribute("prenume", bean.getPrenume());
        request.setAttribute("varsta", bean.getVarsta());

        request.getRequestDispatcher("./info-student.jsp").forward(request, response);
    }
}