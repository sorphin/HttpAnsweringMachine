package org.kendar.replayer.storage;


import org.kendar.servers.db.DbTable;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.util.HashMap;

@Component

@Entity
@Table(name="REPLAYER_RECORDING")
public class DbRecording implements DbTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name="name")
    private String name;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(name="description")
    private String description;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}