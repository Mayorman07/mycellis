package com.mycelis.user.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "authorities",
        indexes = {
                @Index(name = "idx_authorities_name", columnList = "name")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "roles")
public class Authority implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private boolean systemAuthority = false;

    @ManyToMany(mappedBy = "authorities")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // Helper methods
    public void addRole(Role role) {
        this.roles.add(role);
        role.getAuthorities().add(this);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getAuthorities().remove(this);
    }
}
