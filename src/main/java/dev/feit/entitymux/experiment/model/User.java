package dev.feit.entitymux.experiment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    private Long id;

    private String username;

    @OneToMany(mappedBy = "owner")
    private List<Document> documents = new ArrayList<>();

    protected User() {
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public List<Document> getDocuments() {
        return Collections.unmodifiableList(documents);
    }
}
