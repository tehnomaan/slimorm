package eu.miltema.slimorm.test;

import javax.persistence.*;

public class EntityFKey {

	@Id
	@GeneratedValue
	Integer id;

	String name;

	@Column(name = "entity_id")
	@ManyToOne
	EntityWithManualId entity;

	@ManyToOne
	EntityWithTypes entityWithTypes;

	public EntityFKey() {
	}

	public EntityFKey(String name, EntityWithManualId entity, EntityWithTypes et) {
		this.name = name;
		this.entity = entity;
		this.entityWithTypes = et;
	}
}
