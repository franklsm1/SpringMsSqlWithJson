package irish.franklin.springmssqljson;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Properties;

@Testcontainers
@SpringBootTest
@ContextConfiguration(initializers = MsSqlTestContainerHelper.Initializer.class)
public abstract class MsSqlTestContainerHelper {

    @Container
    public static MSSQLServerContainer mssql = new MSSQLServerContainer();

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            ConfigurableEnvironment environment = configurableApplicationContext.getEnvironment();
            Properties props = new Properties();

            props.put("spring.datasource.driver-class-name", mssql.getDriverClassName());
            props.put("spring.datasource.url", mssql.getJdbcUrl());
            props.put("spring.datasource.username", mssql.getUsername());
            props.put("spring.datasource.password", mssql.getPassword());

            environment.getPropertySources().addFirst(new PropertiesPropertySource("myTestDBProps", props));
            configurableApplicationContext.setEnvironment(environment);
        }
    }
}
