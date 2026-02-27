package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static MyDataBase instance;
    final String URL ="jdbc:mysql://localhost:3306/integration-pi";
    final String USER = "root";
    final String PASS = "";
    private Connection cnx;

    private MyDataBase(){
        try {
            this.cnx=DriverManager.getConnection(URL,USER,PASS);
            System.out.println("Connected ....");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }


    }

    public static MyDataBase getInstance(){
        if(instance == null){
            instance = new MyDataBase();
        }
        return instance;
    }


    public Connection getCnx() {
        return cnx;
    }
}
