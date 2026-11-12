package com.example.api.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WmsOutboundResult {

    private String requestId;

    private String recordId;

    private String commodityId;

    private String warehouseId;

    private Integer quantity;

    private String batchNo;

    //true 表示该 requestId 已经处理过，本次未重复扣减
    private boolean idempotent;
}
