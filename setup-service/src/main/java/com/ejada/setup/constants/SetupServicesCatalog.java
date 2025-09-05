package com.ejada.setup.constants;

import java.io.Serializable;

public class SetupServicesCatalog implements Serializable {

        private static final long serialVersionUID = 1L;

        // Catalog
        public static final String SERVICES_PARENT_NAME = "/api/v1";

        public static final String CORE = "/core";

        public static final String LOOKUP = "/lookup";

        // Services


	public static final String GET_SYSTEM_PARAMETER_BY_MODULE_SERVICE = "/getSysParamsByModule";
	public static final String GET_SYSTEM_PARAMETER_BY_NAME_SERVICE = "/getSystemParametersByNAME";
	public static final String GET_CITY_LIST_SERVICE = "/getCityList";
	public static final String GET_CITY_BY_COUNTRY_ID_SERVICE = "/getCitiesByCountryId";
	public static final String GET_COUNTRY_LIST_SERVICE = "/getCountryList";
	public static final String GET_LOOKUP_LIST_SERVICE = "/getLookupList";
	public static final String GET_LOOKUP_LIST_BY_LOOKUP_GROUP_SERVICE = "/getLookupsByLookupGroupCode";

        public static final String GET_APPLICATION_RESOURCES_LIST_SERVICE = "/AppResources";

        public static final String UPLOAD_DOCUMENT_SERVICE = "/uploadDocument";
	
	public static final String GET_DOCUMENT_DETAILS_SERVICE = "/getDocumentDetails";
	public static final String ADD_DOCUMENT_SERVICE = "/addDocument";
	public static final String UPDATE_DOCUMENT_SERVICE = "/updateDocument";
	public static final String GET_DOCUMENT_CATEGORIES_SERVICE = "/getDocumentCategories";




}
