package eu.miltema.slimorm.test;

import javax.persistence.*;

@Table(name = "entity")
public class EntityWithoutId {

	@Column(name = "name")
	public String firstName;

	public Integer count;

	public EntityWithoutId() {
	}

	public EntityWithoutId(String firstName, Integer count) {
		this.firstName = firstName;
		this.count = count;
	}

}
