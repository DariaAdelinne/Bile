<html xmlns:jsp="http://java.sun.com/JSP/Page">
<head>
  <title>Formular student</title>
  <meta charset="UTF-8" />
</head>
<body>
<h3>Formular student</h3>

<!-- Formularul trimite datele catre servletul de adaugare -->
Introduceti datele despre student:
<form action="./process-student" method="post">
  Nume: <input type="text" name="nume" />
  <br />
  Prenume: <input type="text" name="prenume" />
  <br />
  Varsta: <input type="number" name="varsta" />
  <br />
  Medie: <input type="number" step="0.01" name="medie" />
  <br /><br />
  <button type="submit" name="submit">Trimite</button>
</form>
</body>
</html>