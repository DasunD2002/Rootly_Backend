package com.backend.rootly.config;

import com.backend.rootly.domain.CreateCapsuleDomain;
import com.backend.rootly.domain.CreateQuestionCommentDomain;
import com.backend.rootly.domain.CreateQuestionDomain;
import com.backend.rootly.domain.ExplorePlacesRequest;
import com.backend.rootly.domain.ForumVoteDomain;
import com.backend.rootly.domain.ProvinceExploreRequest;
import com.backend.rootly.domain.TranslationLookup;
import com.backend.rootly.domain.InviteContributorDomain;
import com.backend.rootly.domain.UpdateCapsuleDomain;
import com.backend.rootly.domain.UserLogin;
import com.backend.rootly.domain.UserRegister;
import com.backend.rootly.dto.request.CreateCapsuleRequestDTO;
import com.backend.rootly.dto.request.CreateQuestionCommentRequestDTO;
import com.backend.rootly.dto.request.CreateQuestionRequestDTO;
import com.backend.rootly.dto.request.ExplorePlacesRequestDTO;
import com.backend.rootly.dto.request.ProvinceExploreRequestDTO;
import com.backend.rootly.dto.request.InviteContributorRequestDTO;
import com.backend.rootly.dto.request.LoginRequestDTO;
import com.backend.rootly.dto.request.RegisterRequestDTO;
<<<<<<< HEAD
import com.backend.rootly.dto.request.UpdateCapsuleRequestDTO;
=======
import com.backend.rootly.dto.request.TranslationLookupRequestDTO;
import com.backend.rootly.dto.request.ForumVoteRequestDTO;
>>>>>>> d9016c034ff668c739df1e6ab37bd3ced3a65295
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
        modelMapper.createTypeMap(ProvinceExploreRequestDTO.class, ProvinceExploreRequest.class);
        modelMapper.createTypeMap(RegisterRequestDTO.class, UserRegister.class);
        modelMapper.createTypeMap(LoginRequestDTO.class, UserLogin.class);
        modelMapper.createTypeMap(CreateCapsuleRequestDTO.class, CreateCapsuleDomain.class);
        modelMapper.createTypeMap(InviteContributorRequestDTO.class, InviteContributorDomain.class);
<<<<<<< HEAD
        modelMapper.createTypeMap(UpdateCapsuleRequestDTO.class, UpdateCapsuleDomain.class);
=======
        modelMapper.createTypeMap(CreateQuestionRequestDTO.class, CreateQuestionDomain.class);
        modelMapper.createTypeMap(CreateQuestionCommentRequestDTO.class, CreateQuestionCommentDomain.class);
        modelMapper.createTypeMap(ForumVoteRequestDTO.class, ForumVoteDomain.class);
        modelMapper.createTypeMap(TranslationLookupRequestDTO.class, TranslationLookup.class);
>>>>>>> d9016c034ff668c739df1e6ab37bd3ced3a65295

        modelMapper.validate();
        return modelMapper;
    }
}
