package com.xfj.order.biz.handler;

import com.xfj.commons.tool.exception.BaseBusinessException;
import com.xfj.order.biz.context.CreateOrderContext;
import com.xfj.order.biz.context.TransHandlerContext;
import com.xfj.order.entitys.Stock;
import com.xfj.order.mapper.OrderItemMapper;
import com.xfj.order.mapper.StockMapper;
import com.xfj.order.dto.CartProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;


@Component
@Slf4j
public class SubStockHandler extends AbstractTransHandler {
    @Autowired
    StockMapper stockMapper;
    @Autowired
    OrderItemMapper orderItemMapper;

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    @Transactional
    public boolean handle(TransHandlerContext context) {
        CreateOrderContext createOrderContext = (CreateOrderContext) context;
        List<CartProductDto> cartProductDtoList = createOrderContext.getCartProductDtoList();
        //item_ids
        List<String> itemIds = createOrderContext.getBuyProductIds();
        //排序
        itemIds.sort(String::compareTo);
        //一次性锁 ids
        List<Stock> list = stockMapper.findStocksForUpdate(itemIds);
        if (list == null || list.isEmpty()) {
            throw new BaseBusinessException("库存未初始化");
        }
        if (list.size() != itemIds.size()) {
            throw new BaseBusinessException("有商品未初始化库存,请在如下商品id中检查库存状态：" + itemIds.toString());

        }
        list.forEach(stock -> {
            cartProductDtoList.forEach(one -> {
                if (Objects.equals(one.getProductId(), stock.getItemId())) {
                    if (stock.getStockCount() < one.getProductNum()) {
                        throw new BaseBusinessException(stock.getItemId() + "库存不足");
                    }
                    stock.setLockCount(one.getProductNum().intValue());
                    stock.setStockCount(-one.getProductNum());
                    //更改库存状态
                    stockMapper.updateStock(stock);
                    return;
                }
            });
        });
        return true;
    }
}