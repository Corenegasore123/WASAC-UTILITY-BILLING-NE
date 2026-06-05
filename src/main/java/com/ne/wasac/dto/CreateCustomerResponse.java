package com.ne.wasac.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateCustomerResponse {

    private String message;
    private CustomerResponse customer;
}
