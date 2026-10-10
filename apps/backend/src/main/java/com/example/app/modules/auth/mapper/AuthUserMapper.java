package com.example.app.modules.auth.mapper;

import com.example.app.modules.auth.dto.response.AuthUserResponse;
import com.example.app.modules.users.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthUserMapper {

    AuthUserResponse toResponse(User user);
}
