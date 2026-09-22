package com.precisionpath.lab_service.client;

import com.precisionpath.lab_service.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Value("${services.user-service.url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    /**
     * Fetches the caller's own profile by forwarding their JWT to GET /api/users/me.
     */
    public UserProfile getCurrentUser(String authorizationHeader) {

        try {
            UserProfile profile = restClient.get()
                    .uri("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(UserProfile.class);

            if (profile == null) {
                throw new ServiceUnavailableException("User service returned no profile");
            }

            return profile;
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException(
                    "Could not load your profile from the user service. Please try again later."
            );
        }
    }
}
