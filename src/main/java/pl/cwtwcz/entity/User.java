package pl.cwtwcz.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Node("Person")
@Data
@EqualsAndHashCode(exclude = "connections")
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue
    private Long id;
    
    @Property("userId")
    private Integer userId;
    
    @Property("username")
    private String username;
    
    @Relationship(type = "KNOWS", direction = Relationship.Direction.OUTGOING)
    private Set<User> connections = new HashSet<>();
} 