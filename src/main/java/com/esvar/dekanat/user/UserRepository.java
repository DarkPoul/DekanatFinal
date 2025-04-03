package com.esvar.dekanat.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {
    Optional<UserModel> findByEmail(String email);

    UserModel findByLastnameAndFirstnameAndPatronymic(String p, String i, String b);
}
