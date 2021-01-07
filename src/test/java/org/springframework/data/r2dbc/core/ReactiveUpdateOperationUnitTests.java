/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.data.r2dbc.core;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.r2dbc.query.Criteria.*;
import static org.springframework.data.relational.core.query.Query.*;

import io.r2dbc.spi.test.MockResult;
import reactor.test.StepVerifier;

import org.junit.Before;
import org.junit.Test;

import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.mapping.SettableValue;
import org.springframework.data.r2dbc.testing.StatementRecorder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.query.Update;

/**
 * Unit test for {@link ReactiveUpdateOperation}.
 *
 * @author Mark Paluch
 */
public class ReactiveUpdateOperationUnitTests {

	DatabaseClient client;
	R2dbcEntityTemplate entityTemplate;
	StatementRecorder recorder;

	@Before
	public void before() {

		recorder = StatementRecorder.newInstance();
		client = DatabaseClient.builder().connectionFactory(recorder)
				.dataAccessStrategy(new DefaultReactiveDataAccessStrategy(PostgresDialect.INSTANCE)).build();
		entityTemplate = new R2dbcEntityTemplate(client);
	}

	@Test // gh-410
	public void shouldUpdate() {

		MockResult result = MockResult.builder().rowsUpdated(1).build();

		recorder.addStubbing(s -> s.startsWith("UPDATE"), result);

		entityTemplate.update(Person.class) //
				.apply(Update.update("name", "Heisenberg")) //
				.as(StepVerifier::create) //
				.expectNext(1) //
				.verifyComplete();

		StatementRecorder.RecordedStatement statement = recorder.getCreatedStatement(s -> s.startsWith("UPDATE"));

		assertThat(statement.getSql()).isEqualTo("UPDATE person SET THE_NAME = $1");
		assertThat(statement.getBindings()).hasSize(1).containsEntry(0, SettableValue.from("Heisenberg"));
	}

	@Test // gh-410
	public void shouldUpdateWithTable() {

		MockResult result = MockResult.builder().rowsUpdated(1).build();

		recorder.addStubbing(s -> s.startsWith("UPDATE"), result);

		entityTemplate.update(Person.class) //
				.inTable("table").apply(Update.update("name", "Heisenberg")) //
				.as(StepVerifier::create) //
				.expectNext(1) //
				.verifyComplete();

		StatementRecorder.RecordedStatement statement = recorder.getCreatedStatement(s -> s.startsWith("UPDATE"));

		assertThat(statement.getSql()).isEqualTo("UPDATE table SET THE_NAME = $1");
		assertThat(statement.getBindings()).hasSize(1).containsEntry(0, SettableValue.from("Heisenberg"));
	}

	@Test // gh-220
	public void shouldUpdateWithQuery() {

		MockResult result = MockResult.builder().rowsUpdated(1).build();

		recorder.addStubbing(s -> s.startsWith("UPDATE"), result);

		entityTemplate.update(Person.class) //
				.matching(query(where("name").is("Walter"))) //
				.apply(Update.update("name", "Heisenberg")) //
				.as(StepVerifier::create) //
				.expectNext(1) //
				.verifyComplete();

		StatementRecorder.RecordedStatement statement = recorder.getCreatedStatement(s -> s.startsWith("UPDATE"));

		assertThat(statement.getSql()).isEqualTo("UPDATE person SET THE_NAME = $1 WHERE person.THE_NAME = $2");
		assertThat(statement.getBindings()).hasSize(2).containsEntry(0, SettableValue.from("Heisenberg")).containsEntry(1,
				SettableValue.from("Walter"));
	}

	@Test // gh-220
	public void shouldUpdateInTable() {

		MockResult result = MockResult.builder().rowsUpdated(1).build();

		recorder.addStubbing(s -> s.startsWith("UPDATE"), result);

		entityTemplate.update(Person.class) //
				.inTable("the_table") //
				.matching(query(where("name").is("Walter"))) //
				.apply(Update.update("name", "Heisenberg")) //
				.as(StepVerifier::create) //
				.expectNext(1) //
				.verifyComplete();

		StatementRecorder.RecordedStatement statement = recorder.getCreatedStatement(s -> s.startsWith("UPDATE"));

		assertThat(statement.getSql()).isEqualTo("UPDATE the_table SET THE_NAME = $1 WHERE the_table.THE_NAME = $2");
	}

	static class Person {

		@Id String id;

		@Column("THE_NAME") String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
