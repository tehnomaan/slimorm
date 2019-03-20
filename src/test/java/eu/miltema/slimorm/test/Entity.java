package eu.miltema.slimorm.test;

import javax.persistence.*;

public class Entity {

	@Id
	@GeneratedValue
	public Integer id;

	public String name;

	public Integer count;

	public Entity() {
	}

	public Entity(String name, Integer count) {
		this.name = name;
		this.count = count;
	}

}
