package com.duong.profile.repository;

import com.duong.profile.entity.UserProfile;
import com.duong.profile.repository.projection.SuggestedUserRow;
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

    // Mutual friends = me -> A -> cand
    @Query("""
      MATCH (me:user_profile {userId:$me})-[:FOLLOWS]->(:user_profile)-[:FOLLOWS]->(cand:user_profile)
      WHERE cand.userId <> $me
        AND NOT (me)-[:FOLLOWS]->(cand)
      WITH cand, count(*) AS mutuals
      // chỉ trả khi có ít nhất 1 mutual
      WHERE mutuals >= 1
      RETURN cand.userId AS userId,
             cand.username AS username,
             COALESCE(cand.avatar,'') AS avatar,
             mutuals AS mutuals
      ORDER BY mutuals DESC, username ASC
      SKIP $skip LIMIT $limit
    """)
    List<SuggestedUserRow> suggestByMutuals(String me, long skip, long limit);

    @Query("""
      MATCH (me:user_profile {userId:$me})-[:FOLLOWS]->(:user_profile)-[:FOLLOWS]->(cand:user_profile)
      WHERE cand.userId <> $me
        AND NOT (me)-[:FOLLOWS]->(cand)
      RETURN count(DISTINCT cand)
    """)
    long countMutualSuggestions(String me);

    // Fallback: người "phổ biến" theo số followers (khi mutuals không có)
    @Query("""
      MATCH (cand:user_profile)
      WHERE cand.userId <> $me
        AND NOT EXISTS { MATCH (me:user_profile {userId:$me})-[:FOLLOWS]->(cand) }
      OPTIONAL MATCH (f:user_profile)-[:FOLLOWS]->(cand)
      WITH cand, count(f) AS followers
      WHERE followers >= 1
      RETURN cand.userId AS userId,
             cand.username AS username,
             COALESCE(cand.avatar,'') AS avatar,
             followers AS mutuals
      ORDER BY followers DESC, username ASC
      SKIP $skip LIMIT $limit
    """)
    List<SuggestedUserRow> suggestPopular(String me, long skip, long limit);

    @Query("""
      MATCH (cand:user_profile)
      WHERE cand.userId <> $me
        AND NOT EXISTS { MATCH (me:user_profile {userId:$me})-[:FOLLOWS]->(cand) }
      OPTIONAL MATCH (f:user_profile)-[:FOLLOWS]->(cand)
      RETURN count(DISTINCT cand)
    """)
    long countPopularSuggestions(String me);

}
