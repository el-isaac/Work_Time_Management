

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Statement;

public class databaseConnection {
    private static final String url = "jdbc:h2:file:./data/mydb;AUTO_SERVER=TRUE";
    private static final String username = "sa";
    private static final String password = "";

    public static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(url,username,password);
    }

    //this method will create table if they don't exist
    public static void setupDatabase() {
        String createTable = """
                
                           create table if not exists work_time (
                               id int auto_increment primary key not null,
                               date Date not null,
                               start_time Time not null,
                               end_time Time not null,
                               total float,
                               earning float
                           )
                """;

        String dropTable = "drop table work_time";
        try {
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(createTable);
            System.out.println("Datebase setup complete.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
