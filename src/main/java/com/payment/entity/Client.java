package com.payment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Client {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @Column(nullable = false,unique = true)
    private String clientId;


    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean active;



}
