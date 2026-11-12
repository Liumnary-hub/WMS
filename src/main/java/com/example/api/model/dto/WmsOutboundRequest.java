package com.example.api.model.dto;

import lombok.Data;

/**
 * MES 领料出库请求。
 * 商品/仓库优先按 id 匹配，其次 code，最后 name。
 */
@Data
public class WmsOutboundRequest {

    //幂等键，重复请求返回同一条出库记录
    private String requestId;

    private String commodityId;
    private String commodityCode;
    private String commodityName;

    private String warehouseId;
    private String warehouseCode;
    private String warehouseName;

    private Integer quantity;

    private String batchNo;

    private String sourceType;
    private String sourceNo;
    private String sourceItemNo;

    private String description;
}
