package com.leyou.search.client;

import com.leyou.client.BrandClientServer;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("item-service")
public interface BrandClient extends BrandClientServer {
}
