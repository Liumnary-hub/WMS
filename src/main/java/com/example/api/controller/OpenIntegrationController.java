package com.example.api.controller;

import com.example.api.model.dto.WmsInboundRequest;
import com.example.api.model.dto.WmsOutboundRequest;
import com.example.api.model.vo.WmsOutboundResult;
import com.example.api.service.IntegrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 供 MES 调用的开放接口。
 * 鉴权由 IntegrationKeyFilter 校验 X-Integration-Key。
 */
@RestController
@RequestMapping("/api/open")
public class OpenIntegrationController {

    @Resource
    private IntegrationService integrationService;

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> result = new HashMap<>();
        result.put("system", "wms");
        result.put("status", "up");
        return result;
    }

    @PostMapping("/inventory/outbound")
    public WmsOutboundResult outbound(@RequestBody WmsOutboundRequest request) throws Exception {
        return integrationService.outbound(request);
    }

    @PostMapping("/inventory/inbound")
    public WmsOutboundResult inbound(@RequestBody WmsInboundRequest request) throws Exception {
        return integrationService.inbound(request);
    }
}
