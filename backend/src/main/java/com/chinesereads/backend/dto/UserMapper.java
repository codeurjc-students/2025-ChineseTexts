package com.chinesereads.backend.dto;

import java.util.List;

import org.mapstruct.Mapper;

import com.chinesereads.backend.Model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDTO(User user);

    List<UserDTO> toDTO(List<User> user);

    User toDomain(UserDTO userWithPasswordDTO);

    List<User> toDomain(List<UserDTO> users);
}