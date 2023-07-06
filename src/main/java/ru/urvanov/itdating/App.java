package ru.urvanov.itdating;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Hello world!
 *
 */
public class App 
{
    public static  String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    public static String protocol = "jdbc:derby:";
    

    
    public static void main( String[] args ) throws IOException, SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException
    {
        try (Connection conn = App.getConnection()) {
        
            try (Statement statement = conn.createStatement()) {
                createTable(statement);
            } catch (SQLException sqlex) {
                sqlex.printStackTrace();
                checkPersonCount(conn);
            }
        }
        showPerson(new PreferencesSettings());
    }

    private static void checkPersonCount(Connection conn) {
        try (Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery("select count(*) from people")) {
            if (rs.next()) {
                long peopleCount = rs.getLong(1);
                System.out.println("People count = " + peopleCount);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void createTable(Statement statement) throws SQLException {
        statement.execute("create table people("
                + "id int,"
                + "avatarurl varchar(1000),"
                + "avatardata blob,"
                + "name varchar(1000),"
                + "login varchar(1000),"
                + "blog varchar(1000),"
                + "email varchar(1000),"
                + "location varchar(1000),"
                + "bio varchar(1000))");
    }

    private static void showPerson(Settings settings) {
        PersonFrame frame = new PersonFrame(settings);
        frame.setVisible(true);
    }

    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(protocol + "it-dating;create=true", new Properties());
    }
}
