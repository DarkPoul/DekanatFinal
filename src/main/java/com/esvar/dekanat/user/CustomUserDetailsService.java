package com.esvar.dekanat.user;

import com.esvar.dekanat.user.UserModel;
import com.esvar.dekanat.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {


    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        assert userRepository != null;
        Optional<UserModel> UserModelOpt = userRepository.findByEmail(email);
        if (UserModelOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        UserModel UserModel = UserModelOpt.get();
        GrantedAuthority authority = new SimpleGrantedAuthority(UserModel.getRole());
        return new User(UserModel.getEmail(), UserModel.getPassword(), Collections.singleton(authority));
    }

    public String getPIB(String email){
        Optional<UserModel> UserModelOpt = userRepository.findByEmail(email);
        UserModel UserModel = UserModelOpt.get();
        return UserModel.getLastname() + " " + UserModel.getFirstname() + " " + UserModel.getPatronymic();
    }


    public String findById(int i) {
        String pib = "";
        if (userRepository.findById((long)i).isPresent()){
            pib = userRepository.findById((long)i).get().getLastname() + " " + userRepository.findById((long)i).get().getFirstname() + " " + userRepository.findById((long)i).get().getPatronymic();
        }
        return pib;
    }
}
