package com.example.api.service;

import com.example.api.model.dto.WmsInboundRequest;
import com.example.api.model.dto.WmsOutboundRequest;
import com.example.api.model.vo.WmsOutboundResult;

public interface IntegrationService {

    WmsOutboundResult outbound(WmsOutboundRequest request) throws Exception;

    WmsOutboundResult inbound(WmsInboundRequest request) throws Exception;
}
