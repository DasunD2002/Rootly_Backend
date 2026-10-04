package com.backend.rootly.controller;

import com.backend.rootly.entity.AccountData;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.repository.UserRepository;
import com.backend.rootly.service.security.JwtService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises real MongoDB writes through the real JWT security filter.
 * Only documents owned by this test's random user IDs are removed afterwards. */
@SpringBootTest(properties = {"logging.level.org.mongodb.driver=WARN"})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AccountDataIntegrationTests {
    @Autowired private WebApplicationContext context;
    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private JwtService jwtService;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter securityFilter;
    @MockitoBean private UserRepository userRepository;
    private MockMvc mvc;
    private String firstId;
    private String secondId;
    private String firstToken;
    private String secondToken;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        firstId = "persistence-test-" + UUID.randomUUID();
        secondId = "persistence-test-" + UUID.randomUUID();
        firstToken = token(firstId);
        secondToken = token(secondId);
    }

    private String token(String id) {
        UserReg user = new UserReg();
        user.setId(id);
        user.setEmail(id + "@example.invalid");
        user.setRole("USER");
        when(userRepository.existsById(id)).thenReturn(true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return jwtService.generateToken(user.getEmail());
    }

    @AfterEach
    void cleanup() {
        mongoTemplate.remove(Query.query(Criteria.where("userId").in(firstId, secondId)), AccountData.class);
        mongoTemplate.remove(Query.query(Criteria.where("followerId").in(firstId, secondId)), com.backend.rootly.entity.UserFollow.class);
        mongoTemplate.remove(Query.query(Criteria.where("userId").in(firstId, secondId)), com.backend.rootly.entity.CulturalPost.class);
        java.util.List<com.backend.rootly.entity.Capsule> capsules = mongoTemplate.find(
                Query.query(Criteria.where("creatorId").in(firstId, secondId)), com.backend.rootly.entity.Capsule.class);
        for (com.backend.rootly.entity.Capsule capsule : capsules) {
            mongoTemplate.remove(Query.query(Criteria.where("capsuleId").is(capsule.getId())), com.backend.rootly.entity.CapsuleEntry.class);
            mongoTemplate.remove(capsule);
        }
    }

    @Test
    void savesRestoresAndIsolatesAllPrivateFeaturesInMongo() throws Exception {
        for (String key : java.util.List.of("journey", "field-notes", "search-history", "social", "preferences", "translations")) {
            String path = "/api/v1/me/data/" + key;
            mvc.perform(put(path).header("Authorization", "Bearer " + firstToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"version\":0,\"data\":{\"marker\":\"my data\",\"userId\":\"forged-user\"}}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
            mvc.perform(get(path).header("Authorization", "Bearer " + firstToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.marker").value("my data"));
            mvc.perform(get(path).header("Authorization", "Bearer " + secondToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0))
                    .andExpect(jsonPath("$.data.marker").doesNotExist());
            AccountData stored = mongoTemplate.findById(firstId + ":" + key, AccountData.class);
            assertThat(stored).isNotNull();
            assertThat(stored.getUserId()).isEqualTo(firstId);
        }
    }

    @Test
    void rejectsMissingMalformedExpiredTokensAndConcurrentOverwrites() throws Exception {
        String path = "/api/v1/me/data/journey";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        JwtService expired = new JwtService(
                "dGVzdF9zZWNyZXRfa2V5X2Zvcl91bml0X3Rlc3RzX211c3RfYmVfYXRfbGVhc3RfMjU2X2JpdHNfbG9uZw==", -1);
        mvc.perform(get(path).header("Authorization", "Bearer " + expired.generateToken(firstId + "@example.invalid")))
                .andExpect(status().isUnauthorized());
        String data = "{\"version\":0,\"data\":{\"sites\":[]}}";
        mvc.perform(put(path).header("Authorization", "Bearer " + firstToken)
                .contentType(MediaType.APPLICATION_JSON).content(data)).andExpect(status().isOk());
        mvc.perform(put(path).header("Authorization", "Bearer " + firstToken)
                .contentType(MediaType.APPLICATION_JSON).content(data)).andExpect(status().isConflict());
        mvc.perform(put(path).header("Authorization", "Bearer " + firstToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1,\"data\":{\"sites\":[]}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void capsulesAndMemoriesUseTokenOwnershipAndPersistInMongo() throws Exception {
        String result = mvc.perform(post("/api/v1/capsules").header("Authorization", "Bearer " + firstToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test capsule\",\"creatorId\":\"forged\",\"type\":\"family\",\"privacy\":\"private\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String capsuleId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result).path("data").path("id").asText();
        assertThat(mongoTemplate.findById(capsuleId, com.backend.rootly.entity.Capsule.class).getCreatorId()).isEqualTo(firstId);
        String entries = "/api/v1/capsules/" + capsuleId + "/entries";
        mvc.perform(post(entries).header("Authorization", "Bearer " + firstToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"letter\",\"content\":\"A private memory\",\"caption\":\"My letter\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.contributorId").value(firstId));
        mvc.perform(get(entries).header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].content").value("A private memory"));
        mvc.perform(get(entries).header("Authorization", "Bearer " + secondToken)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/capsules/" + capsuleId).header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/capsules/" + capsuleId).header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isNoContent());
        assertThat(mongoTemplate.findById(capsuleId, com.backend.rootly.entity.Capsule.class).getArchived()).isTrue();
        assertThat(mongoTemplate.count(Query.query(Criteria.where("capsuleId").is(capsuleId)), com.backend.rootly.entity.CapsuleEntry.class)).isEqualTo(1);
    }

    @Test
    void sealedMemoriesStayLockedUntilTheirDate() throws Exception {
        com.backend.rootly.entity.Capsule capsule = com.backend.rootly.entity.Capsule.builder().creatorId(firstId)
                .type(com.backend.rootly.enums.CapsuleType.FAMILY).privacy(com.backend.rootly.enums.CapsulePrivacy.PRIVATE)
                .status(com.backend.rootly.enums.CapsuleStatus.OPEN).allowContributions(true)
                .unlockCondition(com.backend.rootly.entity.UnlockCondition.builder()
                        .type(com.backend.rootly.enums.UnlockConditionType.DATE).date(java.time.Instant.now().plusSeconds(3600)).build()).build();
        mongoTemplate.save(capsule);
        String path = "/api/v1/capsules/" + capsule.getId();
        mvc.perform(post(path + "/seal").header("Authorization", "Bearer " + secondToken)).andExpect(status().isNotFound());
        mvc.perform(post(path + "/seal").header("Authorization", "Bearer " + firstToken)).andExpect(status().isOk());
        mvc.perform(get(path + "/entries").header("Authorization", "Bearer " + firstToken)).andExpect(status().isLocked());
        mvc.perform(post(path + "/entries").header("Authorization", "Bearer " + firstToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"letter\",\"content\":\"Too late\"}")).andExpect(status().isConflict());
        capsule = mongoTemplate.findById(capsule.getId(), com.backend.rootly.entity.Capsule.class);
        capsule.getUnlockCondition().setDate(java.time.Instant.now().minusSeconds(1));
        mongoTemplate.save(capsule);
        mvc.perform(get(path + "/entries").header("Authorization", "Bearer " + firstToken)).andExpect(status().isOk());
    }

    @Test
    void followRelationshipsAreIdempotentAndBelongToTheLoggedInUser() throws Exception {
        String path = "/api/v1/users/" + secondId + "/follow";
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(put(path).header("Authorization", "Bearer " + firstToken).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"following\":true}")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.following").value(true)).andExpect(jsonPath("$.data.followerCount").value(1));
        }
        mvc.perform(get("/api/v1/users/" + firstId + "/follow").header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.following").value(false));
        mvc.perform(put(path).header("Authorization", "Bearer " + firstToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"following\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.followerCount").value(0));
    }

    @Test
    void draftsAreStoredUnderTheirAuthorAndHiddenFromOtherUsers() throws Exception {
        String result = mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer " + firstToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"My unfinished draft\",\"visibility\":\"public\",\"isDraft\":true}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String postId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result).path("data").path("id").asText();
        com.backend.rootly.entity.CulturalPost stored = mongoTemplate.findById(postId, com.backend.rootly.entity.CulturalPost.class);
        assertThat(stored.getUserId()).isEqualTo(firstId);
        assertThat(stored.getIsDraft()).isTrue();
        mvc.perform(get("/api/v1/posts/" + postId).header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.isDraft").value(true));
        mvc.perform(get("/api/v1/posts/" + postId).header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/posts/" + postId + "/save").header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/users/" + secondId).header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.email").isEmpty());
    }

    @Test
    void anotherAccountCannotUpdateProfileOrPassword() throws Exception {
        mvc.perform(put("/api/v1/users/" + firstId).header("Authorization", "Bearer " + secondToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"intruder\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/users/" + firstId + "/password").header("Authorization", "Bearer " + secondToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"irrelevant\",\"newPassword\":\"NewPassword123!\"}"))
                .andExpect(status().isForbidden());
    }
}
