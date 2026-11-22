package ma.sinistrepro.indemnities.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class PolicyServiceClientFallbackFactory implements FallbackFactory<PolicyServiceClient> {

    @Override
    public PolicyServiceClient create(Throwable cause) {
        return (policyId, claimType) -> {
            throw RemoteCallFailures.translate("service-polices", cause);
        };
    }
}
