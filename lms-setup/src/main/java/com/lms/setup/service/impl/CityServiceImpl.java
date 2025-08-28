package com.lms.setup.service.impl;

import com.common.dto.BaseResponse;
import com.lms.setup.domain.CitySpecifications;
import com.lms.setup.dto.CityDto;
import com.lms.setup.mapper.CityMapper;
import com.lms.setup.model.City;
import com.lms.setup.model.Country;
import com.lms.setup.repository.CityRepository;
import com.lms.setup.repository.CountryRepository;
import com.lms.setup.service.CityService;
import com.shared.audit.starter.api.AuditAction;
import com.shared.audit.starter.api.DataClass;
import com.shared.audit.starter.api.annotations.Audited;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {
	private static final Logger LOGGER = LoggerFactory.getLogger(CityServiceImpl.class);
	private final CityRepository cityRepo;
	private final CountryRepository countryRepo;
	private final CityMapper mapper;

	@Override
	@Transactional
	public BaseResponse<CityDto> add(CityDto request) {
		Country country = countryRepo.findById(request.getCountryId())
				.orElseThrow(() -> new IllegalArgumentException("Country not found"));

		City entity = mapper.toEntity(request);
		entity.setCountry(country);
		City saved = cityRepo.save(entity);
		return BaseResponse.success("City created", mapper.toDto(saved));
	}

	@Override
	@Transactional
	public BaseResponse<CityDto> update(Integer id, CityDto request) {
		City city = cityRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("City not found"));

		// update mutable fields
		city.setCityCd(request.getCityCd());
		city.setCityEnNm(request.getCityEnNm());
		city.setCityArNm(request.getCityArNm());
		city.setIsActive(request.getIsActive());

		if (!city.getCountry().getCountryId().equals(request.getCountryId())) {
			Country country = countryRepo.findById(request.getCountryId())
					.orElseThrow(() -> new IllegalArgumentException("Country not found"));
			city.setCountry(country);
		}

		City saved = cityRepo.save(city);
		return BaseResponse.success("City updated", mapper.toDto(saved));
	}

	@Override
	@Transactional
	public BaseResponse<Void> delete(Integer id) {
		if (!cityRepo.existsById(id)) {
			return BaseResponse.error("CITY_NOT_FOUND", "City not found");
		}
		cityRepo.deleteById(id);
		return BaseResponse.success("City deleted", null);
	}

	@Override
	@Transactional
	public BaseResponse<CityDto> get(Integer id) {
		City city = cityRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("City not found"));
		return BaseResponse.success("OK", mapper.toDto(city));
	}

	@Override
	@Transactional(Transactional.TxType.SUPPORTS)
	@Audited(action = AuditAction.READ, entity = "City", dataClass = DataClass.HEALTH, message = "List cities")
	public BaseResponse<?> list(Pageable pageable, String q, boolean all) {
		try {
			final Pageable pg = (pageable == null ? Pageable.unpaged() : pageable);

			if (all) {
				var sort = pg.getSort().isSorted() ? pg.getSort() : Sort.by("cityEnNm").ascending();
				var spec = CitySpecifications.nameContains(q); // null => no filter
				var entities = cityRepo.findAll(spec, sort);
				return BaseResponse.success("City list", mapper.toDtoList(entities));
			}

			var spec = Specification.where(CitySpecifications.isActive()).and(CitySpecifications.nameContains(q));

			var page = cityRepo.findAll(spec, pg);
			return BaseResponse.success("Cities page", mapper.toDtoPage(page));

		} catch (Exception ex) {
			LOGGER.error("List cities failed", ex);
			return BaseResponse.error("ERR_CITY_LIST", ex.getMessage());
		}
	}

	@Override
	@Transactional(Transactional.TxType.SUPPORTS)
	@Audited(action = AuditAction.READ, entity = "City", dataClass = DataClass.HEALTH, message = "List active cities by country")
	public BaseResponse<List<CityDto>> listActiveByCountry(Integer countryId) {
		try {
			if (!countryRepo.existsById(countryId)) {
				return BaseResponse.error("ERR_CITY_COUNTRY_NOT_FOUND", "Country not found");
			}
			List<City> entities = cityRepo.findByCountry_CountryIdAndIsActiveTrueOrderByCityEnNmAsc(countryId);

			return BaseResponse.success("Active cities", mapper.toDtoList(entities));
		} catch (Exception ex) {
			LOGGER.error("List active cities failed", ex);
			return BaseResponse.error("ERR_CITY_LIST_ACTIVE", ex.getMessage());
		}
	}
}
