package com.ejada.setup.repository;

import com.ejada.setup.model.Country;
import com.ejada.testsupport.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CountryRepositoryIT extends IntegrationTestSupport {

    @Autowired
    private CountryRepository repository;

    @Test
    void savesAndLoadsCountry() {
        Country country = new Country();
        country.setCountryCd("US");
        country.setCountryEnNm("United States");
        country.setIsActive(true);

        Country saved = repository.save(country);
        assertThat(repository.findById(saved.getCountryId())).isPresent();
    }
}

