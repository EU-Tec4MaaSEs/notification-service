package gr.atc.t4m.utils;

import org.springframework.security.oauth2.jwt.Jwt;

/*
 * Utility class to parse the JWT received token and extract user roles
 */
public class JwtUtils {
    private static final String ID_FIELD = "sub";
    private static final String PILOT_ROLE = "pilot_role";
    private JwtUtils() {}

    /**
     * Util to retrieve pilot role from JWT Token
     *
     * @param jwt Token to extract pilot type
     * @return Pilot Type
     */
    public static String extractPilotRole(Jwt jwt){
        if (jwt == null || jwt.getClaimAsString(PILOT_ROLE) == null) {
            return null;
        }
        return jwt.getClaimAsStringList(PILOT_ROLE).getFirst();
    }

    /**
     * Util to extract the userId from Token
     *
     * @param jwt Token to extract userId
     * @return userId
     */
    public static String extractUserId(Jwt jwt){
        if (jwt == null || jwt.getClaimAsString(ID_FIELD) == null) {
            return null;
        }
        return jwt.getClaimAsString(ID_FIELD);
    }
}