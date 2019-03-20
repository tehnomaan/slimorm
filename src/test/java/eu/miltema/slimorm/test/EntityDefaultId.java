package eu.miltema.slimorm.test;

import javax.persistence.Table;

@Table(name = "entity")
public class EntityDefaultId {
	public Integer id;//Due to name "id", SlimORM must assume @Id and @GeneratedValue automatically

	public String name;

	public Integer count;

}
