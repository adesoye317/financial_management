package com.financal.mgt.Financal.Management.model;


import lombok.Data;
import jakarta.persistence.*;
import org.springframework.data.mongodb.core.index.Indexed;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email", "phoneNumber"})
})
@Data
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;

    @Indexed(unique = true)
    private String phoneNumber;

    @Indexed(unique = true)
    private String email;


    private String passwordHash;
}

