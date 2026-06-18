import beans.StudentBean;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.time.Year;

public class ProcessStudentServlet extends HttpServlet {

    private static final String FILE_PATH =
            "/Users/daria.adelinne/Downloads/glassfish5/student.xml";

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String nume = request.getParameter("nume");
        String prenume = request.getParameter("prenume");
        int varsta = Integer.parseInt(request.getParameter("varsta"));

        int anCurent = Year.now().getValue();
        int anNastere = anCurent - varsta;

        StudentBean bean = new StudentBean();
        bean.setNume(nume);
        bean.setPrenume(prenume);
        bean.setVarsta(varsta);

        XmlMapper mapper = new XmlMapper();
        mapper.writeValue(new File(FILE_PATH), bean);

        request.setAttribute("nume", nume);
        request.setAttribute("prenume", prenume);
        request.setAttribute("varsta", varsta);
        request.setAttribute("anNastere", anNastere);

        request.getRequestDispatcher("./info-student.jsp").forward(request, response);
    }
}