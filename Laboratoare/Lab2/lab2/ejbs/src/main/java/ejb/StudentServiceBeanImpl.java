package ejb;

import interfaces.StudentServiceBeanRemote;

import javax.ejb.Stateful;
import java.io.Serializable;

// @Stateful inseamna ca bean-ul poate pastra stare intre apeluri
// Aici il folosim mai ales ca modulul ejbs sa fie un EJB module valid
@Stateful
public class StudentServiceBeanImpl implements StudentServiceBeanRemote, Serializable {

    // Metoda foarte simpla, folosita doar ca sa avem un bean functional
    @Override
    public String ping() {
        return "Bean OK";
    }
}