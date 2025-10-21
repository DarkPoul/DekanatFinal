package com.esvar.dekanat.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {

    private Long id;
    private String pib;
    private String email;
    private boolean active;
    private String role;
    private String roleType;

}
