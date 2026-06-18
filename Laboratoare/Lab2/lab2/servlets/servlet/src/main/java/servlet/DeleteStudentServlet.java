package servlet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

// Servletul acesta sterge un student din baza de date dupa id
public class DeleteStudentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        // Citim id-ul studentului care trebuie sters
        int id = Integer.parseInt(request.getParameter("id"));

        // Deschidem conexiunea JPA
        EntityManagerFactory factory =
                Persistence.createEntityManagerFactory("bazaDeDateSQLite");
        EntityManager em = factory.createEntityManager();

        // Pornim tranzactia
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        // JPQL delete care sterge studentul cu id-ul primit
        Query query = em.createQuery("delete from StudentEntity s where s.id = :id");
        query.setParameter("id", id);
        query.executeUpdate();

        // Confirmam stergerea
        transaction.commit();

        // Inchidem resursele
        em.close();
        factory.close();

        // Afisam mesajul de succes
        response.setContentType("text/html");
        response.getWriter().println("Studentul a fost sters."
                + "<br /><br /><a href='./fetch-student-list'>Inapoi la lista</a>");
    }
}