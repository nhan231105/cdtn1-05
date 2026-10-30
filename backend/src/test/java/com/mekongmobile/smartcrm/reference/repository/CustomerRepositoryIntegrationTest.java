
package com.mekongmobile.smartcrm.reference.repository;

import com.mekongmobile.smartcrm.reference.entity.Customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CustomerRepositoryIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldFindExistingCustomer() {

        Customer customer = customerRepository
                .findById(201L)
                .orElseThrow();

        assertEquals(201L, customer.getCustomerId());

        assertEquals(
                "0900000001",
                customer.getPhone()
        );
    }

    @Test
    void shouldReturnEmptyForUnknownCustomer() {

        assertTrue(
                customerRepository.findById(999L).isEmpty()
        );
    }
}
