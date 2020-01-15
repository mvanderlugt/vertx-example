package us.vanderlugt.sample.vertx.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HttpStatus {
    OK("OK", 200),
    CREATED("Created", 201),
    ACCEPTED("Accepted", 202),
    NON_AUTHORITATIVE_INFO("Non-Authoritative Information", 203),
    NO_CONTENT("No Content", 204),

    BAD_REQUEST("Bad Request", 400),
    UNAUTHORIZED("Unauthorized", 401),
    FORBIDDEN("Forbidden", 403),
    NOT_FOUND("Not Found", 404),
    METHOD_NOT_ALLOWED("Method Not Allowed", 405),
    CONFLICT("Conflict", 409),
    GONE("Gone", 410),
    IM_A_TEAPOT("I'm a teapot", 418),

    INTERNAL_SERVER_ERROR("Internal Server Error", 500),
    NOT_IMPLEMENTED("Not Implemented", 501),
    SERVICE_UNAVAILABLE("Service Unavailable", 503);

    private final String name;
    private final int code;
}
