package com.ejada.setup.enums;

public final class ECMEnums {

    public enum DocumentAttributeValueType {
        NUMBER("NUMBER"),
        TEXT("TEXT"),
        CHOICE("CHOICE"),
        MULTIPLECHOICES("MULTIPLECHOICES");

        private final String description;

        DocumentAttributeValueType(final String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum ErrorMessage {

        // 200s Status messages.
        OK("INFO_ECM_200_00000", "Success Operation"),
        CREATED("INFO_ECM_201_00001", "data added successfully"),
        UPDATED("INFO_ECM_201_00002", "data updated successfully"),
        RECEIVED("INFO_ECM_201_00003", "data received successfully"),

        // 400s Status Messages
        REQUIRED_DATA_MISSING("ERROR_ECM_400_00001", "{0} is Required"),
        DATA_NOT_EXISTING("ERROR_ECM_400_00002", " {0} with value {1} does not exist"),
        DATA_VALUE_INVALID("ERROR_ECM_400_00003", "{0} : {1} is not valid for type {2}"),
        FILE_EXTENSION_NOT_SUPPORTED("ERROR_ECM_400_00004", "File with extension {0} , not supported"),
        MAX_FILE_SIZE_EXCEEDED("ERROR_ECM_400_00005", "Max File Size Exceeded"),
        MULTIPLE_UUIDS_NOT_ACCEPTABLE("ERROR_ECM_400_00006", "single UUID only allowed"),
        LOCKED_DOCUMENT("ERROR_ECM_400_00007", "Cannot update locked file"),
        DELETED_DOCUMENT("ERROR_ECM_400_00008", "Cannot update deleted file"),
        UNAUTHORIZED("ERROR_ECM_400_00009", "You are not authorized"),




        // 500s Status Messagess
        INTERNAL_SERVER_ERROR("ERROR_ECM_500_00001", "Error While processing an API call to {0}:{1}"),
        GENERAL_ERROR("ERROR_ECM_500_00002", "General Error");

        private final String errorCode;
        private final String description;

        ErrorMessage(final String errorCode, final String description) {
            this.errorCode = errorCode;
            this.description = description;
        }


        public String getDescription() {
            return description;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
