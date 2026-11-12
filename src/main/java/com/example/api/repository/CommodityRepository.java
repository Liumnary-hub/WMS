package com.example.api.repository;

import com.example.api.model.entity.Commodity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommodityRepository extends JpaRepository<Commodity, String> {

    Commodity findByName(String name);

    Commodity findFirstByCode(String code);

    Commodity findFirstByName(String name);

    List<Commodity> findByNameLike(String name);

    //条件扣减，避免读改写导致并发超卖
    @Modifying
    @Query("update Commodity c set c.count = c.count - :count where c.id = :id and c.count >= :count")
    int deductCount(@Param("id") String id, @Param("count") int count);

    @Modifying
    @Query("update Commodity c set c.count = c.count + :count where c.id = :id")
    int increaseCount(@Param("id") String id, @Param("count") int count);

}
