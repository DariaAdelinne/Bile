package servlet;

import ejb.StudentEntity;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import java.util.List;

// Acest thread ruleaza in paralel cu aplicatia
// El verifica periodic baza de date si cauta valori in afara intervalelor admise
public class DatabaseMonitorThread extends Thread {

    // Variabila folosita ca sa putem opri threadul cand se inchide aplicatia
    private boolean running = true;

    // Intervalul permis pentru varsta
    private final int minVarsta = 18;
    private final int maxVarsta = 30;

    // Intervalul permis pentru medie
    private final double minMedie = 5.0;
    private final double maxMedie = 10.0;

    @Override
    public void run() {
        // Cat timp aplicatia ruleaza, threadul continua monitorizarea
        while (running) {
            EntityManagerFactory factory = null;
            EntityManager em = null;

            try {
                // Deschidem conexiunea JPA
                factory = Persistence.createEntityManagerFactory("bazaDeDateSQLite");
                em = factory.createEntityManager();

                // Luam toti studentii din baza de date
                TypedQuery<StudentEntity> query =
                        em.createQuery("select s from StudentEntity s", StudentEntity.class);

                List<StudentEntity> studenti = query.getResultList();

                // Aici construim toate mesajele de alarma gasite
                StringBuilder alarme = new StringBuilder();

                for (StudentEntity s : studenti) {
                    // Verificam daca varsta iese din intervalul [18, 30]
                    if (s.getVarsta() < minVarsta || s.getVarsta() > maxVarsta) {
                        alarme.append("ALARMA: campul 'varsta' are valoarea ")
                                .append(s.getVarsta())
                                .append(" la studentul ")
                                .append(s.getNume()).append(" ")
                                .append(s.getPrenume())
                                .append("<br />");
                    }

                    // Verificam daca media iese din intervalul [5, 10]
                    if (s.getMedie() < minMedie || s.getMedie() > maxMedie) {
                        alarme.append("ALARMA: campul 'medie' are valoarea ")
                                .append(s.getMedie())
                                .append(" la studentul ")
                                .append(s.getNume()).append(" ")
                                .append(s.getPrenume())
                                .append("<br />");
                    }
                }

                // Daca nu exista nicio problema, afisam mesajul standard
                if (alarme.length() == 0) {
                    AlarmData.setMesaj("Nu exista alarme.");
                } else {
                    // Daca exista probleme, salvam toate mesajele gasite
                    AlarmData.setMesaj(alarme.toString());
                }

                // Threadul asteapta 5 secunde intre doua verificari
                Thread.sleep(5000);

            } catch (Exception e) {
                // Daca apare vreo eroare, o salvam ca mesaj de alarma
                AlarmData.setMesaj("Eroare la monitorizare: " + e.getMessage());

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            } finally {
                // Inchidem resursele JPA
                if (em != null) {
                    em.close();
                }
                if (factory != null) {
                    factory.close();
                }
            }
        }
    }

    // Metoda apelata cand vrem sa oprim threadul
    public void stopMonitoring() {
        running = false;
    }
}