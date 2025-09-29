package com.ejada.setup.repository;

import com.ejada.setup.model.Country;
import com.ejada.testsupport.extensions.PostgresTestExtension;
import com.ejada.testsupport.extensions.SetupSchemaExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith({PostgresTestExtension.class, SetupSchemaExtension.class})
@Testcontainers(disabledWithoutDocker = true)
class CountryRepositoryIT {

    @Autowired
    private CountryRepository repository;

    @Test
    void savesAndLoadsCountry() {
        Country country = new Country();
        country.setCountryCd("US");
        country.setCountryEnNm("United States");
        country.setCountryArNm("الولايات المتحدة");
        country.setIsActive(true);

        Country saved = repository.save(country);
        assertThat(repository.findById(saved.getCountryId())).isPresent();
    }
}

