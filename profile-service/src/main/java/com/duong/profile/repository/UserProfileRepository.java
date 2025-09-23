package com.duong.profile.repository;

import com.duong.profile.entity.UserProfile;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends Neo4jRepository<UserProfile, String> {

    // Load profile kèm followers/following để mapper đếm/list chính xác
    @Query("""
      MATCH (p:user_profile {userId:$userId})
      OPTIONAL MATCH (p)-[:FOLLOWS]->(fol:user_profile)
      OPTIONAL MATCH (f:user_profile)-[:FOLLOWS]->(p)
      RETURN p, collect(DISTINCT fol) AS following, collect(DISTINCT f) AS followers
    """)
    Optional<UserProfile> loadProfileGraph(String userId);

    Optional<UserProfile> findByUserId(String userId);

    // search username (LIKE); có thể đổi thành CONTAINS + toLower nếu cần case-insensitive
    List<UserProfile> findAllByUsernameLike(String username);

    // Follow / Unfollow (idempotent)
    @Query("""
      MATCH (a:user_profile {userId:$followerId})
      MATCH (b:user_profile {userId:$targetId})
      MERGE (a)-[:FOLLOWS]->(b)
    """)
    void follow(String followerId, String targetId);

    @Query("""
      MATCH (a:user_profile {userId:$followerId})-[r:FOLLOWS]->(b:user_profile {userId:$targetId})
      DELETE r
    """)
    void unfollow(String followerId, String targetId);

    // List + Count
    @Query("""
      MATCH (:user_profile {userId:$userId})-[:FOLLOWS]->(u:user_profile)
      RETURN u ORDER BY u.username SKIP $skip LIMIT $limit
    """)
    List<UserProfile> findFollowingPage(String userId, long skip, long limit);

    @Query("""
      MATCH (u:user_profile)-[:FOLLOWS]->(:user_profile {userId:$userId})
      RETURN u ORDER BY u.username SKIP $skip LIMIT $limit
    """)
    List<UserProfile> findFollowersPage(String userId, long skip, long limit);

    @Query("""
      MATCH (:user_profile {userId:$userId})-[:FOLLOWS]->(u:user_profile)
      RETURN count(u)
    """)
    long countFollowing(String userId);

    @Query("""
      MATCH (u:user_profile)-[:FOLLOWS]->(:user_profile {userId:$userId})
      RETURN count(u)
    """)
    long countFollowers(String userId);
}
