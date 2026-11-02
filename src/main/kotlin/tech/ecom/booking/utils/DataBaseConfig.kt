package utils

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import tech.ecom.booking.utils.AppConfig
import java.sql.Connection
import javax.sql.DataSource

object DataBaseConfig {

    private val dataSource: DataSource by lazy {
        val config = HikariConfig().apply {
            jdbcUrl = AppConfig.dbUrl
            username = AppConfig.dbUser
            password = AppConfig.dbPassword
            driverClassName = "org.postgresql.Driver"
        }

        HikariDataSource(config)
    }

    val jdbcTemplate: JdbcTemplate by lazy {
        JdbcTemplate(dataSource)
    }

    val namedParameterJdbcTemplate: NamedParameterJdbcTemplate by lazy {
        NamedParameterJdbcTemplate(dataSource)
    }

    fun getConnection(): Connection {
        return dataSource.connection
    }

    fun checkConnection(): Boolean {
        return try {
            jdbcTemplate.queryForObject("SELECT 1", Int::class.java)
            println("✅ База данных успешно подключена")
            true
        } catch (e: Exception) {
            println("❌ Ошибка подключения к базе данных: ${e.message}")
            false
        }
    }
}