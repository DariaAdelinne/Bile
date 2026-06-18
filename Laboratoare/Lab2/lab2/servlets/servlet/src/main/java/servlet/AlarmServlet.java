package servlet;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

// Servletul acesta afiseaza in browser mesajul de alarma calculat de thread
public class AlarmServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html");

        // Afisam o pagina HTML simpla cu mesajul curent din AlarmData
        response.getWriter().println(
                "<html>" +
                        "<head><title>Pagina de alarmare</title><meta charset='UTF-8'></head>" +
                        "<body>" +
                        "<h2>Pagina de alarmare</h2>" +
                        "<p>" + AlarmData.getMesaj() + "</p>" +
                        "<br /><a href='./'>Inapoi la meniul principal</a>" +
                        "</body>" +
                        "</html>"
        );
    }
}