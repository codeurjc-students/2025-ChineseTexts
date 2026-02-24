package com.chinesereads.backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.dto.UserDTO;
import com.chinesereads.backend.dto.UserMapper;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    public UserDTO save(UserDTO user){
        if(userRepository.findByEmail(user.email()).isPresent()){
            return null;
        } else{
            User newUser = userMapper.toDomain(user);
            //newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
            return userMapper.toDTO(userRepository.save(newUser));
        }
    }

}