package eu.miltema.slimorm.test;

import javax.persistence.*;

public class SlimTestEntity {

	@Id
	@GeneratedValue
	public Integer id;

	public String name;

	public Integer count;

	public SlimTestEntity() {
	}

	public SlimTestEntity(String name, Integer count) {
		this.name = name;
		this.count = count;
	}

}
