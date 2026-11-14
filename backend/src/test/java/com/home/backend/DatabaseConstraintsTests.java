package com.home.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.home.backend.entities.Home;
import com.home.backend.entities.HomeUser;
import com.home.backend.entities.Item;
import com.home.backend.entities.QuickMessage;
import com.home.backend.entities.User;
import com.home.backend.entities.enums.HomeRole;
import com.home.backend.entities.enums.Store;
import com.home.backend.repositories.HomeRepository;
import com.home.backend.repositories.ItemRepository;
import com.home.backend.repositories.QuickMessageRepository;
import com.home.backend.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simula o que acontece numa corrida: salva direto pelo repositório, pulando a checagem da Home,
 * e confere que o banco barra mesmo assim.
 */
@SpringBootTest
@Transactional
class DatabaseConstraintsTests {

  @Autowired HomeRepository homeRepository;
  @Autowired ItemRepository itemRepository;
  @Autowired UserRepository userRepository;
  @Autowired QuickMessageRepository quickMessageRepository;
  @Autowired EntityManager entityManager;

  @Test
  void itemNameIsUniquePerHomeIgnoringCaseAndSpaces() {
    Home home = homeRepository.saveAndFlush(new Home("Casa"));
    itemRepository.saveAndFlush(new Item(home, "Arroz", Store.MERCADO));

    assertThrows(DataIntegrityViolationException.class,
        () -> itemRepository.saveAndFlush(new Item(home, "  arroz ", Store.MERCADO)));
  }

  @Test
  void quickMessageIsDeletedWhenAuthorLeavesHome() {
    User user = userRepository.saveAndFlush(new User("test_author"));
    Home home = new Home("Casa");
    HomeUser author = home.addMember(user, HomeRole.MEMBER);
    homeRepository.saveAndFlush(home);

    QuickMessage old = quickMessageRepository.saveAndFlush(
        new QuickMessage(author, "antiga", Instant.now().plusSeconds(60)));
    QuickMessage latest = quickMessageRepository.saveAndFlush(
        new QuickMessage(author, "nova", Instant.now().plusSeconds(60)));
    quickMessageRepository.saveAndFlush(
        new QuickMessage(author, "expirada", Instant.now().minusSeconds(60)));
    entityManager.clear();

    assertEquals(latest.getId(), quickMessageRepository
        .findFirstByHomeIdAndExpiresAtAfterOrderByCreatedAtDesc(home.getId(), Instant.now())
        .orElseThrow().getId());

    homeRepository.findById(home.getId()).orElseThrow().removeMember("test_author");
    entityManager.flush();
    entityManager.clear();

    assertTrue(quickMessageRepository.findById(old.getId()).isEmpty());
    assertTrue(quickMessageRepository.findById(latest.getId()).isEmpty());
  }
}
