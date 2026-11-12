package com.example.api.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * 库存记录 出库 入库
 */
@Data
@Entity
@NoArgsConstructor
public class InventoryRecord {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    //商品名
    private String name;

    //仓库id
    private String wid;

    //商品信息id
    private String cid;

    private Integer count;

    //-1:出库 +1:入库
    private Integer type;

    //描述信息
    private String description;

    private String createAt;

    //外部系统请求号，用于出库接口幂等
    @Column(unique = true)
    private String requestId;

    //批次号（WMS库存本身不按批次记账，这里只留痕）
    private String batchNo;

    //来源系统类型，例如 MES_PICK_LIST
    private String sourceType;

    //来源单号，例如 MES 领料单号
    private String sourceNo;

    //来源明细号，例如 MES 领料明细ID
    private String sourceItemNo;

}
