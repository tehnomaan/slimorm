package eu.miltema.slimorm.test;

import javax.persistence.*;

@Table(name = "entity")
public class EntityForSql {

	public int id;
	public String name;
	public Integer count;
}
