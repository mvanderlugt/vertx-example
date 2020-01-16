package us.vanderlugt.sample.vertx.model;

import lombok.Getter;

@Getter
public class EntityNotFoundException extends RuntimeException {
    private final String id;

    public EntityNotFoundException(String id) {
        super("Entity not found with id " + id);
        this.id = id;
    }
}
