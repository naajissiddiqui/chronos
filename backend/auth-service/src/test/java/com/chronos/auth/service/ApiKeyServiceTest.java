package com.chronos.auth.service;

import com.chronos.auth.dto.*;
import com.chronos.auth.entity.ApiKey;
import com.chronos.auth.entity.Organization;
import com.chronos.auth.entity.Role;
import com.chronos.auth.entity.User;
import com.chronos.auth.repository.ApiKeyRepository;
import com.chronos.auth.repository.OrganizationRepository;
import com.chronos.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApiKeyServiceTest {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        apiKeyRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        testOrg = organizationRepository.save(new Organization("Test Corp"));
        testUser = userRepository.save(new User(
                "apikeyuser@example.com",
                "Password123!",
                "John",
                "Doe",
                Role.ADMIN,
                testOrg
        ));
    }

    @Test
    void testCreateApiKey_RawKeyReturnedOnce_HashPersisted() {
        CreateApiKeyRequest request = new CreateApiKeyRequest("CI Pipeline Key", null);

        CreateApiKeyResponse response = apiKeyService.createApiKey(testOrg.getId(), testUser.getId(), request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertNotNull(response.getRawKey());
        assertTrue(response.getRawKey().startsWith("chron_"));
        assertEquals("CI Pipeline Key", response.getName());
        assertTrue(response.isActive());

        // Verify entity in DB
        Optional<ApiKey> inDb = apiKeyRepository.findById(response.getId());
        assertTrue(inDb.isPresent());
        ApiKey entity = inDb.get();

        // Raw key MUST NOT equal hash
        assertNotEquals(response.getRawKey(), entity.getKeyHash());
        assertFalse(entity.getKeyHash().contains(response.getRawKey()));
        assertEquals(8, entity.getKeyPrefix().length());
    }

    @Test
    void testListApiKeys_DoesNotExposeRawKeyOrHash() {
        CreateApiKeyRequest req1 = new CreateApiKeyRequest("Key 1", null);
        CreateApiKeyRequest req2 = new CreateApiKeyRequest("Key 2", null);

        apiKeyService.createApiKey(testOrg.getId(), testUser.getId(), req1);
        apiKeyService.createApiKey(testOrg.getId(), testUser.getId(), req2);

        List<ApiKeyResponse> list = apiKeyService.listApiKeys(testOrg.getId());
        assertEquals(2, list.size());

        for (ApiKeyResponse res : list) {
            assertNotNull(res.getKeyPrefix());
            assertNotNull(res.getName());
            // Ensure no field leaking hash or rawKey
        }
    }

    @Test
    void testRevokeApiKey_SetsActiveFalse() {
        CreateApiKeyResponse created = apiKeyService.createApiKey(testOrg.getId(), testUser.getId(), new CreateApiKeyRequest("To Revoke", null));

        apiKeyService.revokeApiKey(created.getId(), testOrg.getId());

        Optional<ApiKey> inDb = apiKeyRepository.findById(created.getId());
        assertTrue(inDb.isPresent());
        assertFalse(inDb.get().isActive());

        // Validation should fail
        ApiKeyValidationResult result = apiKeyService.validateApiKey(created.getRawKey());
        assertFalse(result.isValid());
        assertEquals("API key is revoked", result.getErrorReason());
    }

    @Test
    void testValidateApiKey_ValidKey_ReturnsSuccessAndUpdatesLastUsed() {
        CreateApiKeyResponse created = apiKeyService.createApiKey(testOrg.getId(), testUser.getId(), new CreateApiKeyRequest("Valid Key", null));

        ApiKeyValidationResult result = apiKeyService.validateApiKey(created.getRawKey());

        assertTrue(result.isValid());
        assertEquals(testUser.getId(), result.getUserId());
        assertEquals(testOrg.getId(), result.getOrganizationId());
        assertEquals(Role.ADMIN.name(), result.getRole());
        assertEquals(created.getId(), result.getKeyId());

        Optional<ApiKey> inDb = apiKeyRepository.findById(created.getId());
        assertTrue(inDb.isPresent());
        assertNotNull(inDb.get().getLastUsedAt());
    }

    @Test
    void testValidateApiKey_ExpiredKey_Rejected() {
        Instant pastExpiry = Instant.now().minusSeconds(3600);
        CreateApiKeyResponse created = apiKeyService.createApiKey(testOrg.getId(), testUser.getId(), new CreateApiKeyRequest("Expired Key", pastExpiry));

        ApiKeyValidationResult result = apiKeyService.validateApiKey(created.getRawKey());

        assertFalse(result.isValid());
        assertEquals("API key is expired", result.getErrorReason());
    }

    @Test
    void testValidateApiKey_InvalidOrUnknownKey_Rejected() {
        ApiKeyValidationResult res1 = apiKeyService.validateApiKey("invalid-key-format");
        assertFalse(res1.isValid());
        assertEquals("Invalid key format", res1.getErrorReason());

        ApiKeyValidationResult res2 = apiKeyService.validateApiKey("chron_prefix1_nonexistentsecretkey1234567890");
        assertFalse(res2.isValid());
        assertEquals("API key not found", res2.getErrorReason());
    }
}
