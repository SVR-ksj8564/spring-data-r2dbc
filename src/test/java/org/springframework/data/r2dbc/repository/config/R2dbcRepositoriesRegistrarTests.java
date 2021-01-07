/*
 * Copyright 2018-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.r2dbc.repository.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.r2dbc.spi.ConnectionFactory;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.DefaultReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.dialect.MySqlDialect;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.dialect.SqlServerDialect;
import org.springframework.data.r2dbc.repository.config.mysql.MySqlPersonRepository;
import org.springframework.data.r2dbc.repository.config.sqlserver.SqlServerPersonRepository;

/**
 * Integration tests for {@link R2dbcRepositoriesRegistrar}.
 *
 * @author Mark Paluch
 */
public class R2dbcRepositoriesRegistrarTests {

	@Configuration
	@EnableR2dbcRepositories(basePackages = "org.springframework.data.r2dbc.repository.config")
	static class EnableWithDatabaseClient {

		@Bean
		public DatabaseClient r2dbcDatabaseClient() {
			return mock(DatabaseClient.class);
		}

		@Bean
		public ReactiveDataAccessStrategy reactiveDataAccessStrategy() {
			return new DefaultReactiveDataAccessStrategy(PostgresDialect.INSTANCE);
		}
	}

	@Configuration
	@EnableR2dbcRepositories(basePackages = "org.springframework.data.r2dbc.repository.config",
			entityOperationsRef = "myEntityOperations")
	static class EnableWithEntityOperations {

		@Bean
		public R2dbcEntityOperations myEntityOperations() {
			return new R2dbcEntityTemplate(mock(DatabaseClient.class),
					new DefaultReactiveDataAccessStrategy(PostgresDialect.INSTANCE));
		}
	}

	@Configuration
	@EnableR2dbcRepositories(basePackages = "org.springframework.data.r2dbc.repository.config.mysql",
			entityOperationsRef = "mysqlR2dbcEntityOperations")
	static class MySQLConfiguration {

		@Bean
		@Qualifier("mysql")
		public ConnectionFactory mysqlConnectionFactory() {
			return mock(ConnectionFactory.class);
		}

		@Bean
		public R2dbcEntityOperations mysqlR2dbcEntityOperations(@Qualifier("mysql") ConnectionFactory connectionFactory) {

			DefaultReactiveDataAccessStrategy strategy = new DefaultReactiveDataAccessStrategy(MySqlDialect.INSTANCE);
			DatabaseClient databaseClient = DatabaseClient.builder().connectionFactory(connectionFactory)
					.dataAccessStrategy(strategy).build();

			return new R2dbcEntityTemplate(databaseClient, strategy);
		}
	}

	@Configuration
	@EnableR2dbcRepositories(basePackages = "org.springframework.data.r2dbc.repository.config.sqlserver",
			entityOperationsRef = "sqlserverR2dbcEntityOperations")
	static class SQLServerConfiguration {

		@Bean
		public ConnectionFactory sqlserverConnectionFactory() {
			return mock(ConnectionFactory.class);
		}

		@Bean
		public DatabaseClient sqlserverDatabaseClient(
				@Qualifier("sqlserverConnectionFactory") ConnectionFactory connectionFactory,
				@Qualifier("sqlserverDataAccessStrategy") ReactiveDataAccessStrategy mysqlDataAccessStrategy) {
			return DatabaseClient.builder().connectionFactory(connectionFactory).dataAccessStrategy(mysqlDataAccessStrategy)
					.build();
		}

		@Bean
		public R2dbcEntityOperations sqlserverR2dbcEntityOperations(
				@Qualifier("sqlserverDatabaseClient") DatabaseClient mysqlDatabaseClient,
				@Qualifier("sqlserverDataAccessStrategy") ReactiveDataAccessStrategy mysqlDataAccessStrategy) {
			return new R2dbcEntityTemplate(mysqlDatabaseClient, mysqlDataAccessStrategy);
		}

		@Bean
		public ReactiveDataAccessStrategy sqlserverDataAccessStrategy() {
			return new DefaultReactiveDataAccessStrategy(SqlServerDialect.INSTANCE);
		}
	}

	@Test // gh-13
	public void testConfigurationUsingDatabaseClient() {

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				EnableWithDatabaseClient.class)) {

			assertThat(context.getBean(PersonRepository.class)).isNotNull();
		}
	}

	@Test // gh-406
	public void testConfigurationUsingEntityOperations() {

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				EnableWithEntityOperations.class)) {

			assertThat(context.getBean(PersonRepository.class)).isNotNull();
		}
	}

	@Test // gh-406
	public void testMultipleDatabases() {

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MySQLConfiguration.class,
				SQLServerConfiguration.class)) {

			assertThat(context.getBean(MySqlPersonRepository.class)).isNotNull();
			assertThat(context.getBean(SqlServerPersonRepository.class)).isNotNull();
		}
	}
}
