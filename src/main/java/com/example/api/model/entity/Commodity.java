package com.example.api.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * 商品
 */
@Data
@Entity
@NoArgsConstructor
public class Commodity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String name;

    //外部系统物料编码（与MES material_code对应）
    private String code;

    private double price;

    private String description;

    private int count;

    private String createAt;

    private String updateAt;

}
