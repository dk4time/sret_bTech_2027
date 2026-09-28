package com.ridehailing.service;

import com.ridehailing.model.common.Money;
import com.ridehailing.model.user.Customer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Registers customers and handles wallet top-ups. */
public class CustomerService {

    private final List<Customer> customers = new ArrayList<>();

    public Customer register(Customer customer) {
        for (Customer existing : customers) {
            if (existing.getPhone().equals(customer.getPhone())) {
                throw new IllegalArgumentException(customer.getPhone() + " is already registered");
            }
        }
        customers.add(customer);
        return customer;
    }

    /** Positive amounts only, at most ₹10,000 per transaction (checked inside Customer). */
    public void topUpWallet(Customer customer, Money amount) {
        customer.topUpWallet(amount);
    }

    public List<Customer> getAllCustomers() {
        return Collections.unmodifiableList(customers);
    }
}
