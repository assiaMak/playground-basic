import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class SampleClientTest {

    private ListAppender<ILoggingEvent> listAppender;

    @Before
    public void setUp(){
        Logger testLogger = (Logger) LoggerFactory.getLogger(SampleClient.class);
        listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        testLogger.addAppender(listAppender);
    }

    @Test
    public void main_should_log_avgResponseTime() throws IOException, URISyntaxException {
        SampleClient.main(new String[0]);

        List<Integer> avgResponseTimeList = listAppender.list
                .stream()
                .filter(event -> event.getFormattedMessage().startsWith("Average response time :"))
                .map(extractAvgResponseTime())
                .collect(Collectors.toList());

        assertThat(avgResponseTimeList.size()).isEqualTo(3);
        assertThat(avgResponseTimeList.get(0)).isGreaterThan(0);
    }

    @Test
    public void main_second_request_time_should_be_shorter() throws IOException, URISyntaxException {
        SampleClient.main(new String[0]);

        List<ILoggingEvent> requestTimeEventList = listAppender.list
                .stream()
                .filter(event -> event.getFormattedMessage().startsWith("Average response time :"))
                .collect(Collectors.toList());

        assertThat(getSecondLoopLogMessage(requestTimeEventList).endsWith(getMinRequestTime(requestTimeEventList).toString())).isTrue();
    }

    private Integer getMinRequestTime(List<ILoggingEvent> requestTimeEventList) {
        return requestTimeEventList
                .stream()
                .map(extractAvgResponseTime())
                .min(Comparator.comparing(Integer::valueOf))
                .orElse(null);
    }

    private String getSecondLoopLogMessage(List<ILoggingEvent> requestTimeEventList) {
        return requestTimeEventList
                .get(1)
                .getFormattedMessage();
    }

    private Function<ILoggingEvent, Integer> extractAvgResponseTime() {
        return event -> Integer.valueOf(event.getFormattedMessage().split(":")[1].trim());
    }

}