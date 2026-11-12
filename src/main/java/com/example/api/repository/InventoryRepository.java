package com.example.api.repository;

import com.example.api.model.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {

    Inventory findByWidAndCid(String wid, String cid);

    List<Inventory> findAllByCid(String cid);

    List<Inventory> findAllByWid(String wid);

    //条件扣减，库存不足时更新0行
    @Modifying
    @Query("update Inventory i set i.count = i.count - :count " +
            "where i.wid = :wid and i.cid = :cid and i.count >= :count")
    int deductCount(@Param("wid") String wid, @Param("cid") String cid, @Param("count") int count);

    @Modifying
    @Query("update Inventory i set i.count = i.count + :count " +
            "where i.wid = :wid and i.cid = :cid")
    int increaseCount(@Param("wid") String wid, @Param("cid") String cid, @Param("count") int count);

}
