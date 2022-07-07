package org.toasthub.stock.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.toasthub.common.BaseEntity;

@Entity
@Table(name = "ta_configuration")
public class Configuration extends BaseEntity {
    private boolean backloaded = false;

    @Column(name = "backloaded")
    public boolean isBackloaded() {
        return backloaded;
    }

    public void setBackloaded(boolean backloaded) {
        this.backloaded = backloaded;
    }

}
