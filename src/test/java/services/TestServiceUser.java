package services;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import models.UserModels.Role;
import models.UserModels.user;
import services.UserServices.serviceUser;

import java.sql.SQLException;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestServiceUser {
    private static serviceUser su ;

    @BeforeAll
    static void setup(){
         su = new serviceUser();

    }

    @Test
    @Order(1)
    void TestajouterUser() throws SQLException {
        user u1=new user("mimo","maknouch","mimo@gmail.com","testpassword","2003-05-05","2026-02-02",Role.ETUDIANT);
        su.add(u1);
        List<user> list = su.getAll();
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(user->user.getUser_nom().equals("mimo")));

    }

    @Test
    @Order(2)
    void TestGetOneById(){
        List<user> list = su.getAll();
        user u1 = list.get(list.size()-1);
        user u2=su.getOneById(u1.getUser_id());
        assertNotNull(u2);
        assertEquals("mimo",u2.getUser_nom());




    }

    @Test
    @Order(3)
    void TestUpdateById() throws SQLException{
        su.updateById(13,"TESTmimo","TESTmaknouch","TEST.mimo@GMAIL.COM","mimops","2003-05-05","2026-02-02",Role.ETUDIANT);
        List<user> list = su.getAll();
        boolean trouve =list.stream().anyMatch(user->user.getUser_nom().equals("TESTmimo"));
        assertTrue(trouve);


    }

    @Test
    @Order(4)
    void TestDeleteById(){
        su.deleteById(13);
        List<user> List = su.getAll();
        assertNull(su.getOneById(13),"user not found");

    }

//    @AfterEach
//    void cleanup(){
//        try{
//            List<user> users = su.getAll();
//            if(!users.isEmpty()){
//                su.deleteById(users.get(users.size()-1).getUser_id());
//            }
//        }catch (Exception e){
//            System.err.println(e.getMessage());
//        }
//    }






}
