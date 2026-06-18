package servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

// Listenerul se executa automat la pornirea si oprirea aplicatiei web
public class AppContextListener implements ServletContextListener {

    private DatabaseMonitorThread monitorThread;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Cand porneste aplicatia, pornim si threadul de monitorizare
        monitorThread = new DatabaseMonitorThread();
        monitorThread.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Cand se inchide aplicatia, oprim threadul
        if (monitorThread != null) {
            monitorThread.stopMonitoring();
        }
    }
}