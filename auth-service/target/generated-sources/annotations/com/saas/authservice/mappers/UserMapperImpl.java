package com.saas.authservice.mappers;

import com.saas.authservice.dto.internal.ThirdPartyBasicInfoDTO;
import com.saas.authservice.dto.request.user.EmbeddedUserRequestDTO;
import com.saas.authservice.dto.response.LoginResponseDTO;
import com.saas.authservice.dto.response.UserResponseDTO;
import com.saas.authservice.entities.User;
import com.saas.authservice.models.UserModel;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-13T22:42:32-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.1 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserModel requestToModel(EmbeddedUserRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        UserModel userModel = new UserModel();

        userModel.setUsername( dto.getUsername() );
        userModel.setPassword( dto.getPassword() );
        userModel.setEmail( dto.getEmail() );
        userModel.setCellular( dto.getCellular() );

        return userModel;
    }

    @Override
    public User modelToEntity(UserModel model) {
        if ( model == null ) {
            return null;
        }

        User user = new User();

        user.setId( model.getId() );
        user.setUsername( model.getUsername() );
        user.setPassword( model.getPassword() );
        user.setEmail( model.getEmail() );
        user.setCellular( model.getCellular() );

        return user;
    }

    @Override
    public UserModel entityToModel(User entity) {
        if ( entity == null ) {
            return null;
        }

        UserModel userModel = new UserModel();

        userModel.setId( entity.getId() );
        userModel.setUsername( entity.getUsername() );
        userModel.setEmail( entity.getEmail() );
        userModel.setCellular( entity.getCellular() );

        return userModel;
    }

    @Override
    public UserResponseDTO modelToResponseDTO(UserModel model) {
        if ( model == null ) {
            return null;
        }

        UUID id = null;
        String username = null;
        String cellular = null;
        String email = null;

        id = model.getId();
        username = model.getUsername();
        cellular = model.getCellular();
        email = model.getEmail();

        UserResponseDTO userResponseDTO = new UserResponseDTO( id, username, cellular, email );

        return userResponseDTO;
    }

    @Override
    public LoginResponseDTO toLoginResponseDTO(User user, ThirdPartyBasicInfoDTO thirdPartyInfo) {
        if ( user == null && thirdPartyInfo == null ) {
            return null;
        }

        LoginResponseDTO loginResponseDTO = new LoginResponseDTO();

        if ( user != null ) {
            loginResponseDTO.setRoles( mapRolesToStrings( user.getRoles() ) );
            loginResponseDTO.setId( user.getId() );
            loginResponseDTO.setUsername( user.getUsername() );
            loginResponseDTO.setEmail( user.getEmail() );
        }
        loginResponseDTO.setName( determineDisplayNameHelper(user, thirdPartyInfo) );

        return loginResponseDTO;
    }
}
