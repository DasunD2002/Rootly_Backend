package com.backend.rootly.config;

import com.backend.rootly.domain.CreateCapsuleDomain;
import com.backend.rootly.domain.ExplorePlacesRequest;
import com.backend.rootly.domain.InviteContributorDomain;
import com.backend.rootly.domain.UpdateCapsuleDomain;
import com.backend.rootly.domain.UserLogin;
import com.backend.rootly.domain.UserRegister;
import com.backend.rootly.dto.request.CreateCapsuleRequestDTO;
import com.backend.rootly.dto.request.ExplorePlacesRequestDTO;
import com.backend.rootly.dto.request.InviteContributorRequestDTO;
import com.backend.rootly.dto.request.LoginRequestDTO;
import com.backend.rootly.dto.request.RegisterRequestDTO;
import com.backend.rootly.dto.request.UpdateCapsuleRequestDTO;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MapperConfiguration {

    @Bean
    // ModelMapper exposes matching settings through its configuration object.
    @SuppressWarnings("PMD.LawOfDemeter")
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.getConfiguration().setSkipNullEnabled(true);

        modelMapper.createTypeMap(ExplorePlacesRequestDTO.class, ExplorePlacesRequest.class);
        modelMapper.createTypeMap(RegisterRequestDTO.class, UserRegister.class);
        modelMapper.createTypeMap(LoginRequestDTO.class, UserLogin.class);
        modelMapper.createTypeMap(CreateCapsuleRequestDTO.class, CreateCapsuleDomain.class);
        modelMapper.createTypeMap(InviteContributorRequestDTO.class, InviteContributorDomain.class);
        modelMapper.createTypeMap(UpdateCapsuleRequestDTO.class, UpdateCapsuleDomain.class);

        modelMapper.validate();
        return modelMapper;
    }
}
