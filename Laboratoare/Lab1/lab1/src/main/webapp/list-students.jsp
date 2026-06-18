<%@ page import="java.util.List" %> <!-- importarea claselor-->
<%@ page import="beans.StudentBean" %>
<html>
<head>
  <title>Lista studenti</title>
  <meta charset="UTF-8">
</head>
<body>

<h2>Lista studenti</h2>

<form action="search-student" method="get">
  Cauta dupa nume/prenume:
  <input type="text" name="q">
  <button type="submit">Cauta</button>
</form>

<br>
<a href="formular.jsp">Adauga student</a>
<br><br>
<a href="export-json">Export JSON</a>
<br><br>

<table border="1">
  <tr>
    <td>ID</td>
    <td>Nume</td>
    <td>Prenume</td>
    <td>Varsta</td>
    <td>Actualizare</td>
    <td>Stergere</td>
  </tr>

  <% //scriptlet ca sa citim atributele
    List<StudentBean> students = (List<StudentBean>) request.getAttribute("students"); //se ia atributul students din request si ce converteste la o lisya de StudentBean
    if (students != null) {
      for (StudentBean s : students) {
  %>
  <tr>
    <form action="update-student" method="post">
      <td>
        <input type="hidden" name="id" value="<%= s.getId() %>"> <!-- se afiseaza ID-ul studentului in tabel si se trimite ascuns in formular ca sa stie servletul ce student sa modifice-->
        <%= s.getId() %>
      </td>
      <td><input type="text" name="nume" value="<%= s.getNume() %>"></td>
      <td><input type="text" name="prenume" value="<%= s.getPrenume() %>"></td>
      <td><input type="number" name="varsta" value="<%= s.getVarsta() %>"></td>
      <td><button type="submit">Update</button></td> <!--catre update-student-->
    </form>
    <td>
      <form action="delete-student" method="post">
        <input type="hidden" name="id" value="<%= s.getId() %>">
        <button type="submit">Delete</button>  <!--catre delete-student-->
      </form>
    </td>
  </tr>
  <%
      }
    }
  %>
</table>

<br>
<a href="index.jsp">Inapoi</a>

</body>
</html>