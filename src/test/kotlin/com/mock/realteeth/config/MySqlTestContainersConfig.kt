package com.mock.realteeth.config

import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

@Configuration
class MySqlTestContainersConfig {
    companion object {
        private val mysqlContainer: MySQLContainer<*> =
            MySQLContainer(DockerImageName.parse("mysql:8.0"))
                .apply {
                    withDatabaseName("realteeth")
                    withUsername("root")
                    withPassword("1234")
                    withInitScript("init-db.sql")
                    withReuse(true)
                    start()
                }

        init {
            val r2dbcUrl = "r2dbc:mysql://${mysqlContainer.host}:${mysqlContainer.firstMappedPort}/realteeth"
            System.setProperty("spring.r2dbc.url", r2dbcUrl)
            System.setProperty("spring.r2dbc.username", mysqlContainer.username)
            System.setProperty("spring.r2dbc.password", mysqlContainer.password)
        }
    }
}
