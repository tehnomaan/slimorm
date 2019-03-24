package eu.miltema.slimorm.test;

import javax.persistence.Id;

public class EntityWithManualId {

	@Id
	public Long id;

	public String name;

	public Integer fInt1;

	public EntityWithManualId() {
	}

	public EntityWithManualId(Long id, String name, Integer fInt1) {
		this.id = id;
		this.name = name;
		this.fInt1 = fInt1;
	}
}
