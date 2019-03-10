package eu.miltema.slimorm.test;

import javax.persistence.*;

@Table(name = "slim_test_entity")
public class SlimTestEntityWithoutId {

	@Column(name = "name")
	public String firstName;

	public Integer count;

	public SlimTestEntityWithoutId() {
	}

	public SlimTestEntityWithoutId(String firstName, Integer count) {
		this.firstName = firstName;
		this.count = count;
	}

}
