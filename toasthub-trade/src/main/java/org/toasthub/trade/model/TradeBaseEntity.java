package org.toasthub.trade.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.toasthub.core.general.api.View;
import org.toasthub.core.general.model.BaseEntity;

import com.fasterxml.jackson.annotation.JsonView;

@MappedSuperclass()
public class TradeBaseEntity extends BaseEntity {

	private static final long serialVersionUID = 1L;
	protected String identifier;

	// Constructor
	public TradeBaseEntity() {
	}
	
	// Setter/Getters
	@JsonView({View.Member.class})
	@Column(name = "identifier")
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
}
