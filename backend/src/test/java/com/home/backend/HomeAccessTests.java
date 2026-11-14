package com.home.backend;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home.backend.clients.ClerkClient;
import com.home.backend.clients.ClerkUser;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// @Transactional: cada teste faz rollback no fim, nada fica no banco
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HomeAccessTests {

  @Autowired MockMvc mvc;

  @Autowired EntityManager entityManager;

  @MockitoBean JwtDecoder jwtDecoder;

  // simula a Backend API do Clerk: todo id existe, com username = id
  @MockitoBean ClerkClient clerkClient;

  // o valor do token vira o "sub" do JWT: "Bearer test_owner" -> usuário test_owner
  @BeforeEach
  void setUp() {
    given(jwtDecoder.decode(anyString()))
        .willAnswer(invocation -> Jwt.withTokenValue(invocation.getArgument(0))
            .header("alg", "RS256")
            .subject(invocation.getArgument(0))
            .claim("username", invocation.getArgument(0))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build());

    given(clerkClient.getUser(anyString()))
        .willAnswer(invocation -> {
          String id = invocation.getArgument(0);
          return Optional.of(new ClerkUser(id, id, "Nome", id, null, "email_1",
              List.of(new ClerkUser.EmailAddress("email_1", id + "@example.com"))));
        });
  }

  // cada request roda "do zero", como em produção (cada request é uma transação nova)
  private ResultActions perform(String userId, MockHttpServletRequestBuilder request) throws Exception {
    entityManager.flush();
    entityManager.clear();
    return mvc.perform(request.header("Authorization", "Bearer " + userId).contentType(MediaType.APPLICATION_JSON));
  }

  private long idFrom(ResultActions result) throws Exception {
    return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
  }

  private long createHome(String ownerId) throws Exception {
    return idFrom(perform(ownerId, post("/homes").content("{\"name\": \"Casa\"}")).andExpect(status().isCreated()));
  }

  private void addMember(long homeId, String ownerId, String memberId) throws Exception {
    long inviteId = idFrom(perform(ownerId, post("/invites")
            .content("{\"homeId\": " + homeId + ", \"inviteeId\": \"" + memberId + "\"}"))
        .andExpect(status().isCreated()));
    perform(memberId, post("/invites/" + inviteId + "/accept")).andExpect(status().isOk());
  }

  private long createItem(long homeId, String userId, String name) throws Exception {
    return idFrom(perform(userId, post("/homes/" + homeId + "/items")
            .content("{\"name\": \"" + name + "\", \"store\": \"MERCADO\"}"))
        .andExpect(status().isCreated()));
  }

  private ResultActions addToList(long homeId, String userId, long itemId, int quantity) throws Exception {
    return perform(userId, post("/homes/" + homeId + "/grocery-list")
        .content("{\"itemId\": " + itemId + ", \"quantity\": " + quantity + "}"));
  }

  @Test
  void nonMemberGets404() throws Exception {
    long homeId = createHome("test_owner");

    perform("test_stranger", get("/homes/" + homeId)).andExpect(status().isNotFound());
    perform("test_stranger", get("/homes/" + homeId + "/items")).andExpect(status().isNotFound());
    perform("test_stranger", get("/homes/" + homeId + "/grocery-list")).andExpect(status().isNotFound());
    perform("test_stranger", put("/homes/" + homeId).content("{\"name\": \"X\"}")).andExpect(status().isNotFound());
  }

  @Test
  void memberWhoIsNotOwnerGets403OnOwnerActions() throws Exception {
    long homeId = createHome("test_owner");
    addMember(homeId, "test_owner", "test_member");

    perform("test_member", put("/homes/" + homeId).content("{\"name\": \"X\"}")).andExpect(status().isForbidden());
    perform("test_owner", put("/homes/" + homeId).content("{\"name\": \"Nova\"}")).andExpect(status().isOk());
  }

  @Test
  void duplicateItemNameGets409() throws Exception {
    long homeId = createHome("test_owner");
    createItem(homeId, "test_owner", "Arroz");

    perform("test_owner", post("/homes/" + homeId + "/items").content("{\"name\": \"arroz\", \"store\": \"MERCADO\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void groceryListMergesPendingAndLocksBoughtEntries() throws Exception {
    long homeId = createHome("test_owner");
    addMember(homeId, "test_owner", "test_member");
    long itemId = createItem(homeId, "test_owner", "Arroz");

    // duas adições do mesmo item -> mesma linha, quantidade somada
    long entryId = idFrom(addToList(homeId, "test_owner", itemId, 2).andExpect(status().isOk()));
    addToList(homeId, "test_member", itemId, 1)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(entryId))
        .andExpect(jsonPath("$.quantity").value(3));

    // compra: guarda quem comprou; depois disso a linha não muda mais
    perform("test_member", post("/homes/" + homeId + "/grocery-list/" + entryId + "/buy"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bought").value(true))
        .andExpect(jsonPath("$.boughtBy").value("test_member"));
    perform("test_owner", patch("/homes/" + homeId + "/grocery-list/" + entryId).content("{\"quantity\": 5}"))
        .andExpect(status().isConflict());

    // adicionar de novo depois da compra -> linha nova
    long newEntryId = idFrom(addToList(homeId, "test_owner", itemId, 1).andExpect(status().isOk()));
    org.junit.jupiter.api.Assertions.assertNotEquals(entryId, newEntryId);

    perform("test_owner", get("/homes/" + homeId + "/grocery-list?bought=false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void inviteFetchesInviteeProfileFromClerk() throws Exception {
    long homeId = createHome("test_owner");

    perform("test_owner", post("/invites")
            .content("{\"homeId\": " + homeId + ", \"inviteeId\": \"test_guest\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.invitee.username").value("test_guest"))
        .andExpect(jsonPath("$.invitee.fullName").value("Nome test_guest"))
        .andExpect(jsonPath("$.invitee.email").value("test_guest@example.com"));

    given(clerkClient.getUser("test_ghost")).willReturn(Optional.empty());
    perform("test_owner", post("/invites")
            .content("{\"homeId\": " + homeId + ", \"inviteeId\": \"test_ghost\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void quickMessageFlow() throws Exception {
    long homeId = createHome("test_owner");
    String url = "/homes/" + homeId + "/quick-message";

    perform("test_owner", get(url)).andExpect(status().isNoContent());

    perform("test_owner", post(url).content("{\"message\": \"Cheguei!\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Cheguei!"))
        .andExpect(jsonPath("$.author.username").value("test_owner"));

    perform("test_owner", get(url))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Cheguei!"))
        .andExpect(jsonPath("$.author.id").value("test_owner"));

    perform("test_owner", post(url).content("{\"message\": \"123456789012345678901\"}"))
        .andExpect(status().isBadRequest());
    perform("test_stranger", get(url)).andExpect(status().isNotFound());
  }
}
