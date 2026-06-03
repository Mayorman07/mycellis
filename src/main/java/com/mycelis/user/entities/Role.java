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
        name = "roles",
        indexes = {
                @Index(name = "idx_roles_name", columnList = "name")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"users", "authorities"})
public class Role implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private boolean systemRole = false;

    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    private Set<User> users = new HashSet<>();

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "roles_authorities",
            joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id", referencedColumnName = "id")
    )
    @Builder.Default
    private Set<Authority> authorities = new HashSet<>();

    // Helper methods
    public void addAuthority(Authority authority) {
        this.authorities.add(authority);
        authority.getRoles().add(this);
    }

    public void removeAuthority(Authority authority) {
        this.authorities.remove(authority);
        authority.getRoles().remove(this);
    }

    public boolean hasAuthority(String authorityName) {
        return this.authorities.stream()
                .anyMatch(a -> a.getName().equalsIgnoreCase(authorityName));
    }
}
