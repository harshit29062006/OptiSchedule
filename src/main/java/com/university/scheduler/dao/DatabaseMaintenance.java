package com.university.scheduler.dao;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public final class DatabaseMaintenance {
    private DatabaseMaintenance() {}
    public static void resetSchema(ConnectionProvider provider){run(provider,"reset-database.sql");SchemaManager.createSchema(provider);}
    private static void run(ConnectionProvider provider,String resource){
        try(InputStream in=DatabaseMaintenance.class.getClassLoader().getResourceAsStream(resource)){
            if(in==null)throw new FileNotFoundException(resource+" is missing from the classpath");
            String sql=new String(in.readAllBytes(),StandardCharsets.UTF_8).replaceAll("(?m)^\\s*--.*$","");
            try(Connection c=provider.getConnection();Statement s=c.createStatement()){for(String part:sql.split(";"))if(!part.isBlank())s.execute(part);}
        }catch(IOException|SQLException e){throw new DatabaseException("Could not run database script "+resource,e);}
    }
}
