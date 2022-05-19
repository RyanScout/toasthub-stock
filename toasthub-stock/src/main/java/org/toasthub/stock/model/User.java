package org.toasthub.stock.model;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.toasthub.common.BaseEntity;

@Entity
@Table(name = "ta_user")
public class User extends BaseEntity{
    private Set<UserTradeSignalKey> userTradeSignalKeys = new LinkedHashSet<UserTradeSignalKey>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    public Set<UserTradeSignalKey> getUserTradeSignalKeys() {
        return userTradeSignalKeys;
    }

    public void setUserTradeSignalKeys(Set<UserTradeSignalKey> userTradeSignalKeys) {
        this.userTradeSignalKeys = userTradeSignalKeys;
    }

}
