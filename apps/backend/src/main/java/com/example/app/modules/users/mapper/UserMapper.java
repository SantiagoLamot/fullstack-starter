package com.example.app.modules.users.mapper;

import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.dto.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
