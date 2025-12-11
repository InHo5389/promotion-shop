package userservice.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultToken;

@Configuration
public class VaultConfig {

    @Value("${spring.vault.token}")
    private String vaultToken;

    @Value("${spring.vault.scheme}")
    private String vaultScheme;

    @Value("${spring.vault.host}")
    private String vaultHost;

    @Value("${spring.vault.port}")
    private int vaultPort;

    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint endpoint = VaultEndpoint.create(vaultHost, vaultPort);
        endpoint.setScheme(vaultScheme);
        // vaultScheme: http, https

        return new VaultTemplate(endpoint, () -> VaultToken.of(vaultToken));
    }
}
