package org.toasthub.stock.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.toasthub.core.general.model.BaseEntity;

@MappedSuperclass()
public class TradeBaseEntity extends BaseEntity {

	private static final long serialVersionUID = 1L;
	protected String identifier;

	// Constructor
	public TradeBaseEntity() {
	}
	
	// Setter/Getters
	@Column(name = "identifier")
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
}
