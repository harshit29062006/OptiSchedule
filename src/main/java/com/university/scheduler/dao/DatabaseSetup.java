package com.university.scheduler.dao;

import java.nio.file.Path;

/** Command line entry point for creating/resetting the schema and importing CSV data. */
public final class DatabaseSetup {
    private DatabaseSetup(){}
    public static void main(String[] args){
        if(args.length==0)throw new IllegalArgumentException("Usage: DatabaseSetup create | reset | import <csv-path>");
        JdbcConnectionFactory db=new JdbcConnectionFactory();
        switch(args[0].toLowerCase()){
            case "create": SchemaManager.createSchema(db);break;
            case "reset": DatabaseMaintenance.resetSchema(db);break;
            case "import": if(args.length<2)throw new IllegalArgumentException("import requires a CSV path");TimetableImporter.importCsv(Path.of(args[1]),db);break;
            default: throw new IllegalArgumentException("Unknown command: "+args[0]);
        }
    }
}
