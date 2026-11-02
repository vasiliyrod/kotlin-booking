package utils

import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.DatabaseException
import liquibase.resource.ClassLoaderResourceAccessor
import kotlin.use


object LiquibaseUtil {
    private val CHANGELOG_PATH = "liquibase/changelog-master.yaml"

    fun applyMigrations() {
        DataBaseConfig.getConnection().use { connection ->
            try {
                val database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(JdbcConnection(connection))

                val liquibase = Liquibase(
                    CHANGELOG_PATH,
                    ClassLoaderResourceAccessor(),
                    database
                )

                liquibase.update()
            } catch (e: Exception) {
                connection.rollback()
                throw DatabaseException("Migration failed", e)
            }
        }
    }
}