package org.toasthub.stock.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.toasthub.common.BaseEntity;

@Entity
@Table(name = "ta_user_trade_signal_key")
public class UserTradeSignalKey extends BaseEntity {
    private String userTradeSignalKey;
    private User user;

    @JsonIgnore
    @ManyToOne(targetEntity = User.class , fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public User getUser() {
        return user;
    }

    public String getUserTradeSignalKey() {
        return userTradeSignalKey;
    }

    public void setUserTradeSignalKey(String userTradeSignalKey) {
        this.userTradeSignalKey = userTradeSignalKey;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
