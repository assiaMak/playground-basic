import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

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
    public void main_should_get_client_SMITH(){
        SampleClient.main(new String[0]);

        Assertions.assertThat(listAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
                .contains(Tuple.tuple("Client request: GET http://hapi.fhir.org/baseR4/Patient?family=SMITH HTTP/1.1", Level.INFO));
    }

}