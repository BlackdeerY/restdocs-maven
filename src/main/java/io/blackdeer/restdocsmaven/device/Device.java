package io.blackdeer.restdocsmaven.device;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.blackdeer.restdocsmaven.hub.Hub;

import javax.persistence.*;

@Entity
@SequenceGenerator(name = "SEQ_DEVICE", sequenceName = "device_sequence")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DEVICE")
    private Long id;

    private String name;

    @ManyToOne
    @JsonBackReference
    private Hub hub;

    public Device() {}

    public Device(Long id, String name, Hub hub) {
        this.id = id;
        this.name = name;
        this.hub = hub;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Hub getHub() {
        return hub;
    }

    public void setHub(Hub hub) {
        this.hub = hub;
    }
}
