package com.esvar.dekanat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "session_control")
public class SessionEntity {
    @Id
    private Long id;
    @Column(nullable = false)
    private boolean isWinter; // true - зимова, false - літня
}
