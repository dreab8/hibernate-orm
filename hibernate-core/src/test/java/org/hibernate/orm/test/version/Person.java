/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import org.hibernate.annotations.OptimisticLock;

import static jakarta.persistence.CascadeType.ALL;

@Entity
public class Person {
	@Id
	private String name;

	@OneToMany(mappedBy = "person", cascade = ALL, orphanRemoval = true)
	@OptimisticLock(excluded = false)
	private List<Thing> things;

	@OneToMany(mappedBy = "person", cascade = ALL, orphanRemoval = true)
	@OptimisticLock(excluded = true)
	private List<Task> tasks;

	@Version
	@Column(name = "`version`")
	private int version;

	Person() {}
	public Person(String name) {
		this.name = name;
		this.things = new ArrayList<>();
		this.tasks = new ArrayList<>();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<Thing> getThings() {
		return things;
	}
	public void setThings(List<Thing> things) {
		this.things = things;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public List<Task> getTasks() {
		return tasks;
	}
	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}
}
