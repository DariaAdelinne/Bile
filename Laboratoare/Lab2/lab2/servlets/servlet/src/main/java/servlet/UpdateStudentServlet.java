package servlet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

// Servletul acesta modifica datele unui student existent dupa id
public class UpdateStudentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {

        // Citim datele primite din formularul de update
        int id = Integer.parseInt(request.getParameter("id"));
        String nume = request.getParameter("nume");
        String prenume = request.getParameter("prenume");
        int varsta = Integer.parseInt(request.getParameter("varsta"));
        double medie = Double.parseDouble(request.getParameter("medie"));

        // Deschidem conexiunea JPA
        EntityManagerFactory factory =
                Persistence.createEntityManagerFactory("bazaDeDateSQLite");
        EntityManager em = factory.createEntityManager();

        // Pornim tranzactia
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        // JPQL update care modifica studentul cu id-ul primit
        Query query = em.createQuery(
                "update StudentEntity s set s.nume = :nume, s.prenume = :prenume, s.varsta = :varsta, s.medie = :medie where s.id = :id"
        );
        query.setParameter("nume", nume);
        query.setParameter("prenume", prenume);
        query.setParameter("varsta", varsta);
        query.setParameter("medie", medie);
        query.setParameter("id", id);
        query.executeUpdate();

        // Confirmam modificarile in baza de date
        transaction.commit();

        // Inchidem resursele
        em.close();
        factory.close();

        // Afisam mesajul de succes
        response.setContentType("text/html");
        response.getWriter().println("Studentul a fost actualizat."
                + "<br /><br /><a href='./fetch-student-list'>Inapoi la lista</a>");
    }
}