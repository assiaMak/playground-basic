import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;

import java.util.List;
import java.util.Optional;

public class CustomLoggingInterceptor implements IClientInterceptor {

    private List<Long> responseTimes;

    @Override
    public void interceptRequest(IHttpRequest iHttpRequest) {

    }

    @Override
    public void interceptResponse(IHttpResponse iHttpResponse) {
        Optional.ofNullable(responseTimes)
                .ifPresent(times -> times.add(iHttpResponse.getRequestStopWatch().getMillis()));
    }

    public void setResponseTimes(List<Long> responseTimes) {
        this.responseTimes = responseTimes;
    }

    public List<Long> getResponseTimes() {
        return responseTimes;
    }
}
