package org.toasthub.trade.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonView;

@Entity
@Table(name = "ta_configuration")
public class Configuration extends TradeBaseEntity {
    private static final long serialVersionUID = 1L;
	private boolean backloaded = false;

	// Setter/Getter
	@JsonView({View.Member.class})
    @Column(name = "backloaded")
    public boolean isBackloaded() {
        return backloaded;
    }

    public void setBackloaded(boolean backloaded) {
        this.backloaded = backloaded;
    }

}
