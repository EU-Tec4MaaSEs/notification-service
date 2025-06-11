package gr.atc.t4m.exception;

public class CustomExceptions {

    private CustomExceptions(){}

    /*
     * Exception thrown when requested data is not found in DB
     */
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    /*
     * Exception thrown when an exception occurs in the mapping process between DTO and Models
     */
    public static class ModelMappingException extends RuntimeException{
        public ModelMappingException(String message){
            super(message);
        }
    }

    /*
     * Exception thrown when a user (except Super-Admins) try to retrieve a Notification belonging to another User
     */
    public static class ForbiddenAccessException extends RuntimeException{
        public ForbiddenAccessException(String message){
            super(message);
        }
    }

    /*
     * Exception thrown when a resource already exists in DB
     */
    public static class ResourceAlreadyExists extends RuntimeException{
        public ResourceAlreadyExists(String message){
            super(message);
        }
    }


}
