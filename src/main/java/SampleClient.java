import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SampleClient {
    private static final Logger logger = LoggerFactory.getLogger(SampleClient.class);
    public static final String NAMES_FILE = "names.txt";

    private IGenericClient client;
    private final CustomLoggingInterceptor clientInterceptor;

    public SampleClient(){
        clientInterceptor = new CustomLoggingInterceptor();
    }

    public static void main(String[] theArgs) throws IOException, URISyntaxException {

        SampleClient sampleClient = new SampleClient();

        // Search for Patient resources
        List<Bundle> responseList = sampleClient.getPatients(sampleClient.getCachedPatients());
        responseList.addAll(sampleClient.getPatients(sampleClient.getCachedPatients()));
        responseList.addAll(sampleClient.getPatients(sampleClient.getNonCachedPatients()));

        responseList.stream()
                .map(Bundle::getEntry)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .map(entry -> (Patient) entry.getResource())
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(p -> getFirstName(p).orElse("")))
                .forEach(patient -> logger.info("patient infos : first name : {}, last name : {}, birth date : {}", getFirstName(patient).orElse(null), getLastName(patient).orElse(null), patient.getBirthDate()));
    }

    private static Optional<String> getLastName(Patient patient) {
        return Optional.of(patient.getName())
                .flatMap(names -> names.stream()
                        .filter(Objects::nonNull)
                        .findFirst()
                        .map(HumanName::getFamily)
                );
    }

    private static Optional<String> getFirstName(Patient patient) {
        return Optional.of(patient.getName())
                .flatMap(names -> names.stream()
                        .filter(Objects::nonNull)
                        .findFirst()
                        .map(HumanName::getGiven)
                        .map(SampleClient::getFirstGivenName)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(StringType::getValue)
                );
    }

    private static Optional<StringType> getFirstGivenName(List<StringType> givenNames) {
        return givenNames.stream()
                .filter(Objects::nonNull)
                .findFirst();
    }


    private Stream<String> getNames() throws IOException, URISyntaxException {
        Path path = Paths.get(
                Optional.ofNullable(
                        ClassLoader.getSystemClassLoader().getResource(NAMES_FILE)
                )
                .orElseThrow(FileNotFoundException::new)
                .toURI()
        );

        return Files.lines(path);
    }


    private List<Bundle> getPatients(Function<String, Bundle> mapNameToBundle) throws IOException, URISyntaxException {
        clientInterceptor.setResponseTimes(new ArrayList<>());

        List<Bundle> patients = getNames()
                .map(mapNameToBundle)
                .collect(Collectors.toList());

        logger.info("Average response time : {}", getAvgResponseTimes());
        return patients;
    }

    private Function<String, Bundle> getCachedPatients() {
        return patientName -> getPatient(Patient.FAMILY.matches().value(patientName.toUpperCase()))
                                .execute();
    }

    private Function<String, Bundle> getNonCachedPatients() {
        return patientName -> getPatient(Patient.FAMILY.matches().value(patientName.toUpperCase()))
                                .cacheControl(new CacheControlDirective().setNoCache(true))
                                .execute();
    }

    private IQuery<Bundle> getPatient(ICriterion<StringClientParam> criterion) {
        return getGenericClient()
                .search()
                .forResource("Patient")
                .where(criterion)
                .returnBundle(Bundle.class);
    }

    private IGenericClient getGenericClient() {
        if (client == null){
            // Create a FHIR client
            FhirContext fhirContext = FhirContext.forR4();
            client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
            client.registerInterceptor(clientInterceptor);
        }
        return client;
    }

    private long getAvgResponseTimes(){
        return (long) clientInterceptor.getResponseTimes().stream().mapToLong(t -> t).average().orElse(0L);
    }

}
