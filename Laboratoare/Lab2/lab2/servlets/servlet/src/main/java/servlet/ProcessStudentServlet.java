package servlet;

import ejb.StudentEntity;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

// Servletul acesta primeste datele din formular si adauga studentul in baza de date
public class ProcessStudentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {

        // Citim valorile trimise din formular
        String nume = request.getParameter("nume");
        String prenume = request.getParameter("prenume");
        int varsta = Integer.parseInt(request.getParameter("varsta"));
        double medie = Double.parseDouble(request.getParameter("medie"));

        // Cream factory-ul si conexiunea JPA catre baza de date
        EntityManagerFactory factory =
                Persistence.createEntityManagerFactory("bazaDeDateSQLite");
        EntityManager em = factory.createEntityManager();

        // Construim obiectul StudentEntity cu datele primite din formular
        StudentEntity student = new StudentEntity();
        student.setNume(nume);
        student.setPrenume(prenume);
        student.setVarsta(varsta);
        student.setMedie(medie);

        // Pornim tranzactia si salvam studentul in baza de date
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        em.persist(student);
        transaction.commit();

        // Inchidem resursele
        em.close();
        factory.close();

        // Afisam mesaj de succes in browser
        response.setContentType("text/html");
        response.getWriter().println("Datele au fost adaugate in baza de date."
                + "<br /><br /><a href='./'>Inapoi la meniul principal</a>");
    }
}