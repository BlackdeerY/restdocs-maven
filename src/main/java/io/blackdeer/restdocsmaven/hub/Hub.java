package io.blackdeer.restdocsmaven.hub;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.blackdeer.restdocsmaven.device.Device;
import io.blackdeer.restdocsmaven.user.User;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@SequenceGenerator(name = "SEQ_HUB", sequenceName = "hub_sequence")
public class Hub {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_HUB")
    private Long id;

    private String name;

    @ManyToMany(mappedBy = "hubs")
    @JsonBackReference
//    @JsonIgnore
    private List<User> users = new ArrayList<>();

    @OneToMany
    @JsonManagedReference
    private List<Device> devices = new ArrayList<>();

    public Hub() {}

    public Hub(Long id, String name) {
        this.id = id;
        this.name = name;
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

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }
}
