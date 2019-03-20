package eu.miltema.slimorm.test;

import javax.persistence.*;

public class EntityFKey {

	@Id
	@GeneratedValue
	Integer id;

	String name;

	@Column(name = "entity_id")
	@ManyToOne
	Entity entity;

	public EntityFKey() {
	}

	public EntityFKey(String name, Entity entity) {
		this.name = name;
		this.entity = entity;
	}
}
