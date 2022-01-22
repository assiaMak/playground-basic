import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.ICriterion;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SampleClient {
    private static final Logger logger = LoggerFactory.getLogger(SampleClient.class);
    public static final String NAMES_FILE = "names.txt";

    private IGenericClient client;

    public static void main(String[] theArgs) throws IOException, URISyntaxException {

        SampleClient sampleClient = new SampleClient();

        // Search for Patient resources
        List<Bundle> responseList = sampleClient.getPatients();

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
                Optional.of(
                        ClassLoader.getSystemClassLoader().getResource(NAMES_FILE)
                )
                .orElseThrow(FileNotFoundException::new)
                .toURI()
        );

        return Files.lines(path);
    }


    private List<Bundle> getPatients() throws IOException, URISyntaxException {
        return getNames()
                .map(
                        patientName -> getPatient(Patient.FAMILY.matches().value(patientName.toUpperCase()))
                )
                .collect(Collectors.toList());
    }


    private Bundle getPatient(ICriterion<StringClientParam> criterion) {
        return getGenericClient()
                .search()
                .forResource("Patient")
                .where(criterion)
                .returnBundle(Bundle.class)
                .execute();
    }

    private IGenericClient getGenericClient() {
        if (client == null){
            // Create a FHIR client
            FhirContext fhirContext = FhirContext.forR4();
            client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
            client.registerInterceptor(new LoggingInterceptor(false));
        }
        return client;
    }

}
