<html>
<head>
  <title>Adauga student</title>
  <meta charset="UTF-8">
</head>
<body>

<h2>Adauga student</h2>

<form action="create-student" method="post"> <!-- datele sunt trimise la url-ul create-student prin metoda post (apeleaza CreateStudentServlet)-->
  Nume: <input type="text" name="nume" required>
  <br><br>

  Prenume: <input type="text" name="prenume" required>
  <br><br>

  Varsta: <input type="number" name="varsta" required>
  <br><br>

  <button type="submit">Salveaza</button>
</form>

<br>
<a href="index.jsp">Inapoi</a>

</body>
</html>