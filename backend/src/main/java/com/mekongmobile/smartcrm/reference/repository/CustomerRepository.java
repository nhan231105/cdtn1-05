
package com.mekongmobile.smartcrm.reference.repository;

import com.mekongmobile.smartcrm.reference.entity.Customer;

import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface CustomerRepository
        extends Repository<Customer, Long> {

    Optional<Customer> findById(Long customerId);
}
