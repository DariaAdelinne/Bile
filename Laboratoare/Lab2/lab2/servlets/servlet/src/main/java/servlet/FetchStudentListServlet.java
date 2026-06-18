package servlet;

import ejb.StudentEntity;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

// Servletul acesta citeste toti studentii din baza de date si ii afiseaza intr-un tabel HTML
public class FetchStudentListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        // Deschidem conexiunea JPA
        EntityManagerFactory factory =
                Persistence.createEntityManagerFactory("bazaDeDateSQLite");
        EntityManager em = factory.createEntityManager();

        // StringBuilder este folosit pentru a construi mai usor raspunsul HTML
        StringBuilder responseText = new StringBuilder();
        responseText.append("<h2>Lista studenti</h2>");
        responseText.append("<table border='1'>");
        responseText.append("<thead><tr><th>ID</th><th>Nume</th><th>Prenume</th><th>Varsta</th><th>Medie</th><th>Update</th><th>Delete</th></tr></thead>");
        responseText.append("<tbody>");

        // JPQL care selecteaza toti studentii din tabela
        TypedQuery<StudentEntity> query =
                em.createQuery("select student from StudentEntity student", StudentEntity.class);
        List<StudentEntity> results = query.getResultList();

        // Pentru fiecare student generam cate un rand in tabel
        for (StudentEntity student : results) {
            responseText.append("<tr>");
            responseText.append("<td>").append(student.getId()).append("</td>");
            responseText.append("<td>").append(student.getNume()).append("</td>");
            responseText.append("<td>").append(student.getPrenume()).append("</td>");
            responseText.append("<td>").append(student.getVarsta()).append("</td>");
            responseText.append("<td>").append(student.getMedie()).append("</td>");

            // Linkul de update trimite datele studentului in pagina JSP de actualizare
            responseText.append("<td><a href='./update-student.jsp?id=")
                    .append(student.getId())
                    .append("&nume=").append(student.getNume())
                    .append("&prenume=").append(student.getPrenume())
                    .append("&varsta=").append(student.getVarsta())
                    .append("&medie=").append(student.getMedie())
                    .append("'>Actualizeaza</a></td>");

            // Linkul de delete trimite id-ul studentului catre servletul de stergere
            responseText.append("<td><a href='./delete-student?id=")
                    .append(student.getId())
                    .append("'>Sterge</a></td>");

            responseText.append("</tr>");
        }

        responseText.append("</tbody></table>");
        responseText.append("<br /><br /><a href='./'>Inapoi la meniul principal</a>");

        // Inchidem resursele
        em.close();
        factory.close();

        // Trimitem HTML-ul catre browser
        response.setContentType("text/html");
        response.getWriter().print(responseText.toString());
    }
}