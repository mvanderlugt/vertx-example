package us.vanderlugt.sample.vertx.model;

import lombok.Getter;

@Getter
public class ConflictException extends Throwable {
    private final String id;

    public ConflictException(String id) {
        super("Conflict with existing entity with id = " + id);
        this.id = id;
    }
}
