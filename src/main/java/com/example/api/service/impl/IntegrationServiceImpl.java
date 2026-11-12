package com.example.api.service.impl;

import com.example.api.model.dto.WmsInboundRequest;
import com.example.api.model.dto.WmsOutboundRequest;
import com.example.api.model.entity.Commodity;
import com.example.api.model.entity.Inventory;
import com.example.api.model.entity.InventoryRecord;
import com.example.api.model.entity.Warehouse;
import com.example.api.model.vo.WmsOutboundResult;
import com.example.api.repository.CommodityRepository;
import com.example.api.repository.InventoryRecordRepository;
import com.example.api.repository.InventoryRepository;
import com.example.api.repository.WareHouseRepository;
import com.example.api.service.IntegrationService;
import com.example.api.utils.DataTimeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Service
public class IntegrationServiceImpl implements IntegrationService {

    @Resource
    private CommodityRepository commodityRepository;

    @Resource
    private WareHouseRepository wareHouseRepository;

    @Resource
    private InventoryRepository inventoryRepository;

    @Resource
    private InventoryRecordRepository recordRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsOutboundResult outbound(WmsOutboundRequest request) throws Exception {
        if (request == null || !StringUtils.hasText(request.getRequestId())) {
            throw new Exception("requestId不能为空");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new Exception("出库数量必须大于0");
        }

        //幂等：同一 requestId 直接返回首次结果，不再扣库存
        InventoryRecord existing = recordRepository.findByRequestId(request.getRequestId());
        if (existing != null) {
            return toResult(existing, true);
        }

        Commodity commodity = resolveCommodity(request);
        Warehouse warehouse = resolveWarehouse(request);
        int quantity = request.getQuantity();

        Inventory inventory = inventoryRepository.findByWidAndCid(warehouse.getId(), commodity.getId());
        if (inventory == null) {
            throw new Exception("WMS仓库内不存在该商品");
        }
        if (inventory.getCount() < quantity) {
            throw new Exception("WMS库存不足");
        }

        //数据库条件更新，防止并发下扣成负数
        if (inventoryRepository.deductCount(warehouse.getId(), commodity.getId(), quantity) == 0) {
            throw new Exception("WMS库存不足");
        }
        if (commodityRepository.deductCount(commodity.getId(), quantity) == 0) {
            throw new Exception("WMS商品总库存不足");
        }

        InventoryRecord record = new InventoryRecord();
        record.setRequestId(request.getRequestId());
        record.setName(commodity.getName());
        record.setWid(warehouse.getId());
        record.setCid(commodity.getId());
        record.setCount(quantity);
        record.setType(-1);
        record.setBatchNo(request.getBatchNo());
        record.setSourceType(request.getSourceType());
        record.setSourceNo(request.getSourceNo());
        record.setSourceItemNo(request.getSourceItemNo());
        record.setDescription(buildDescription(request, commodity, warehouse));
        record.setCreateAt(DataTimeUtil.getNowTimeString());
        recordRepository.save(record);

        return toResult(record, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsOutboundResult inbound(WmsInboundRequest request) throws Exception {
        if (request == null || !StringUtils.hasText(request.getRequestId())) {
            throw new Exception("requestId不能为空");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new Exception("入库数量必须大于0");
        }

        InventoryRecord existing = recordRepository.findByRequestId(request.getRequestId());
        if (existing != null) {
            return toResult(existing, true);
        }

        Commodity commodity = resolveCommodity(request);
        Warehouse warehouse = resolveWarehouse(request);
        int quantity = request.getQuantity();

        Inventory inventory = inventoryRepository.findByWidAndCid(warehouse.getId(), commodity.getId());
        if (inventory == null) {
            inventory = new Inventory();
            inventory.setWid(warehouse.getId());
            inventory.setCid(commodity.getId());
            inventory.setName(commodity.getName());
            inventory.setCount(quantity);
            inventoryRepository.save(inventory);
        } else if (inventoryRepository.increaseCount(warehouse.getId(), commodity.getId(), quantity) == 0) {
            throw new Exception("WMS库存更新失败");
        }

        if (commodityRepository.increaseCount(commodity.getId(), quantity) == 0) {
            throw new Exception("WMS商品总库存更新失败");
        }

        InventoryRecord record = new InventoryRecord();
        record.setRequestId(request.getRequestId());
        record.setName(commodity.getName());
        record.setWid(warehouse.getId());
        record.setCid(commodity.getId());
        record.setCount(quantity);
        record.setType(1);
        record.setBatchNo(request.getBatchNo());
        record.setSourceType(request.getSourceType());
        record.setSourceNo(request.getSourceNo());
        record.setSourceItemNo(request.getSourceItemNo());
        record.setDescription(buildDescription(request, commodity, warehouse));
        record.setCreateAt(DataTimeUtil.getNowTimeString());
        recordRepository.save(record);

        return toResult(record, false);
    }

    private Commodity resolveCommodity(WmsOutboundRequest request) throws Exception {
        if (StringUtils.hasText(request.getCommodityId())) {
            Commodity commodity = commodityRepository.findById(request.getCommodityId()).orElse(null);
            if (commodity != null) {
                return commodity;
            }
        }
        if (StringUtils.hasText(request.getCommodityCode())) {
            Commodity commodity = commodityRepository.findFirstByCode(request.getCommodityCode());
            if (commodity != null) {
                return commodity;
            }
        }
        if (StringUtils.hasText(request.getCommodityName())) {
            Commodity commodity = commodityRepository.findFirstByName(request.getCommodityName());
            if (commodity != null) {
                return commodity;
            }
        }
        throw new Exception("WMS中找不到对应商品: " + request.getCommodityCode());
    }

    private Warehouse resolveWarehouse(WmsOutboundRequest request) throws Exception {
        if (StringUtils.hasText(request.getWarehouseId())) {
            Warehouse warehouse = wareHouseRepository.findById(request.getWarehouseId()).orElse(null);
            if (warehouse != null) {
                return warehouse;
            }
        }
        if (StringUtils.hasText(request.getWarehouseCode())) {
            Warehouse warehouse = wareHouseRepository.findFirstByCode(request.getWarehouseCode());
            if (warehouse != null) {
                return warehouse;
            }
        }
        if (StringUtils.hasText(request.getWarehouseName())) {
            Warehouse warehouse = wareHouseRepository.findFirstByName(request.getWarehouseName());
            if (warehouse != null) {
                return warehouse;
            }
        }
        throw new Exception("WMS中找不到对应仓库: " + request.getWarehouseCode());
    }

    private String buildDescription(WmsOutboundRequest request, Commodity commodity, Warehouse warehouse) {
        if (StringUtils.hasText(request.getDescription())) {
            return request.getDescription();
        }
        return "集成出库: " + commodity.getName() + " / " + warehouse.getName();
    }

    private WmsOutboundResult toResult(InventoryRecord record, boolean idempotent) {
        return new WmsOutboundResult(
                record.getRequestId(),
                record.getId(),
                record.getCid(),
                record.getWid(),
                record.getCount(),
                record.getBatchNo(),
                idempotent
        );
    }
}
