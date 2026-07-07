package com.chinesereads.backend.dto;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.chinesereads.backend.Model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // termsAccepted is a client->server signup input only; the entity stores the proof
    // as termsAcceptedAt, so there is nothing to map back onto the DTO.
    @Mapping(target = "termsAccepted", ignore = true)
    UserDTO toDTO(User user);

    List<UserDTO> toDTO(List<User> user);

    User toDomain(UserDTO userWithPasswordDTO);

    List<User> toDomain(List<UserDTO> users);
}