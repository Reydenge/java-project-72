package hexlet.code;

import io.ebean.annotation.Platform;
import io.ebean.dbmigration.DbMigration;
import io.ebean.annotation.Platform;

import java.io.IOException;

public class MigrationGenerator {
    public static void main(String[] args) throws IOException {
        DbMigration dbMigration = DbMigration.create();

        dbMigration.setPlatform(Platform.POSTGRES);
        dbMigration.setPlatform(Platform.H2);

        dbMigration.generateMigration();
    }
}
