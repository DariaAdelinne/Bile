<%@ page contentType="text/html; charset=UTF-8" %>
<html>
<head>
  <title>Actualizare student</title>
  <meta charset="UTF-8" />
</head>
<body>
<h2>Actualizare student</h2>

<!-- Formularul preia valorile existente din URL si le trimite catre servletul de update -->
<form action="./update-student" method="post">
  <!-- id-ul este ascuns pentru ca nu vrem sa il modifice utilizatorul -->
  <input type="hidden" name="id" value="<%= request.getParameter("id") %>" />

  Nume:
  <input type="text" name="nume" value="<%= request.getParameter("nume") %>" />
  <br />

  Prenume:
  <input type="text" name="prenume" value="<%= request.getParameter("prenume") %>" />
  <br />

  Varsta:
  <input type="number" name="varsta" value="<%= request.getParameter("varsta") %>" />
  <br />

  Medie:
  <input type="number" step="0.01" name="medie" value="<%= request.getParameter("medie") %>" />
  <br /><br />

  <button type="submit">Actualizeaza</button>
</form>
</body>
</html>