
package com.mekongmobile.smartcrm.reference.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "customer")
@Immutable
public class Customer {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "phone")
    private String phone;

    protected Customer() {
        // Required by JPA
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getPhone() {
        return phone;
    }
}
