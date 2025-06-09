package pl.cwtwcz.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.cwtwcz.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends Neo4jRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @Query("MATCH (start:Person {username: $startUsername}), (end:Person {username: $endUsername}) " +
            "MATCH path = shortestPath((start)-[:KNOWS*]-(end)) " +
            "UNWIND nodes(path) as node " +
            "RETURN node.username")
    List<String> findShortestPath(
            @Param("startUsername") String startUsername,
            @Param("endUsername") String endUsername);

    @Query("MATCH (n:Person) DETACH DELETE n")
    void deleteAllUserAndRelationships();
}