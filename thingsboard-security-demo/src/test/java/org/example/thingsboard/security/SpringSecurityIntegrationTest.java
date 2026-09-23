package org.example.thingsboard.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.thingsboard.server.service.security.auth.mfa.provider.impl.TotpTwoFaProvider;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SpringSecurityIntegrationTest {
    private static final Pattern TOKEN = Pattern.compile("\\\"token\\\":\\\"([^\\\"]+)\\\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TotpTwoFaProvider totpProvider;

    @Test
    void securityFilterChainAuthenticatesBearerJwtAndChecksTenant() throws Exception {
        mockMvc.perform(get("/api/devices/tenant-1"))
                .andExpect(status().isUnauthorized());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tenant@example.com\",\"password\":\"tenant-password\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Matcher matcher = Pattern.compile("\\\"preVerificationToken\\\":\\\"([^\\\"]+)\\\"")
                .matcher(login.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new AssertionError("Login response does not contain a pre-verification token");
        }
        String preVerificationToken = matcher.group(1);
        String code = totpProvider.codeAt("JBSWY3DPEHPK3PXP", System.currentTimeMillis() / 1000);
        MvcResult mfa = mockMvc.perform(post("/api/auth/mfa")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preVerificationToken\":\"" + preVerificationToken
                                + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        matcher = TOKEN.matcher(mfa.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new AssertionError("MFA response does not contain an access token");
        }
        String token = matcher.group(1);

        mockMvc.perform(get("/api/devices/tenant-1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/devices/tenant-2").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void springSecurityRegistersOauth2AuthorizationEndpoint() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/demo-provider"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("idp.example/authorize")));
    }
}
