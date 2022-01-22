import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class SampleClientTest {

    private ListAppender<ILoggingEvent> listAppender;

    @Before
    public void setUp(){
        Logger testLogger = (Logger) LoggerFactory.getLogger(LoggingInterceptor.class);
        listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        testLogger.addAppender(listAppender);
    }

    @Test
    public void main_should_call_client_20times() throws IOException, URISyntaxException {
        SampleClient.main(new String[0]);

        List<ILoggingEvent> listEvents = listAppender.list
                .stream()
                .filter(event -> event.getFormattedMessage().startsWith("Client request: GET http://hapi.fhir.org/baseR4/Patient?family="))
                .collect(Collectors.toList());

        assertThat(listEvents.size()).isEqualTo(20);
    }

}