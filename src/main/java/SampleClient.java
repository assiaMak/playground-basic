import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SampleClient {
    private static final Logger logger = LoggerFactory.getLogger(SampleClient.class);

    public static void main(String[] theArgs) {

        // Create a FHIR client
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        client.registerInterceptor(new LoggingInterceptor(false));

        // Search for Patient resources
        Bundle response = client
                .search()
                .forResource("Patient")
                .where(Patient.FAMILY.matches().value("SMITH"))
                .returnBundle(Bundle.class)
                .execute();

        response.getEntry()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(entry -> (Patient) entry.getResource())
                        .filter(Objects::nonNull)
                        .forEach(patient -> logger.info("patient infos : first name : {}, last name : {}, birth date : {}", getFirstName(patient), getLastName(patient), patient.getBirthDate()));
    }

    private static String getLastName(Patient patient) {
        return Optional.of(patient.getName())
                .flatMap(names -> names.stream()
                        .filter(Objects::nonNull)
                        .findFirst()
                        .map(HumanName::getFamily)
                )
                .orElse(null);
    }

    private static StringType getFirstName(Patient patient) {
        return Optional.of(patient.getName())
                .flatMap(names -> names.stream()
                        .filter(Objects::nonNull)
                        .findFirst()
                        .map(HumanName::getGiven)
                        .map(SampleClient::getFirstGivenName)
                )
                .orElse(null);
    }

    private static StringType getFirstGivenName(List<StringType> givenNames) {
       return givenNames.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }


}
