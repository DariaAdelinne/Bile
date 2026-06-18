<html>
<head>
  <title>Informatii student</title>
  <meta charset="UTF-8">
</head>
<body>

<h3>Informatii student</h3>

<ul>
  <li>Nume: <%= request.getAttribute("nume") %></li>
  <li>Prenume: <%= request.getAttribute("prenume") %></li>
  <li>Varsta: <%= request.getAttribute("varsta") %></li>
  <li>An nastere: <%= request.getAttribute("anNastere") %></li>
</ul>

<hr>

<h3>Actualizare student</h3>

<form action="./update-student" method="post">
  Nume:
  <input type="text" name="nume" value="<%= request.getAttribute("nume") %>"/>
  <br/>

  Prenume:
  <input type="text" name="prenume" value="<%= request.getAttribute("prenume") %>"/>
  <br/>

  Varsta:
  <input type="number" name="varsta" value="<%= request.getAttribute("varsta") %>"/>
  <br/><br/>

  <button type="submit">Actualizeaza</button>
</form>

<br/>

<form action="./delete-student" method="post">
  <button type="submit">Sterge student</button>
</form>

<br/>
<a href="formular.jsp">Inapoi la formular</a>

</body>
</html>