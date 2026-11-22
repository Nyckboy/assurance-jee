package ma.sinistrepro.indemnities.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ClaimServiceClientFallbackFactory implements FallbackFactory<ClaimServiceClient> {

    @Override
    public ClaimServiceClient create(Throwable cause) {
        return id -> {
            throw RemoteCallFailures.translate("service-sinistres", cause);
        };
    }
}
