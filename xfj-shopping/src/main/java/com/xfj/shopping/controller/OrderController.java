package com.xfj.shopping.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xfj.commons.result.ResponseData;
import com.xfj.commons.result.ResponseUtil;
import com.xfj.order.OrderCoreService;
import com.xfj.order.OrderQueryService;
import com.xfj.order.constant.OrderRetCode;
import com.xfj.order.rs.CreateOrderRS;
import com.xfj.order.rs.OrderDetailRS;
import com.xfj.order.rs.OrderListRS;
import com.xfj.order.vo.*;
import com.xfj.shopping.form.OrderDetail;
import com.xfj.shopping.form.PageInfo;
import com.xfj.shopping.form.PageResponse;
import com.xfj.user.intercepter.TokenIntercepter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@RestController
@RequestMapping("/shopping")
@Api(tags = "OrderController", description = "订单控制层")
public class OrderController {

    @Reference(timeout = 3000, group = "${dubbo-group.name}")
    private OrderCoreService orderCoreService;

    @Reference(timeout = 3000, group = "${dubbo-group.name}")
    private OrderQueryService orderQueryService;

    /**
     * 创建订单
     */
    @PostMapping("/order")
    @ApiOperation("创建订单")
    public ResponseData order(@RequestBody CreateOrderVO request, HttpServletRequest servletRequest) {
        String userInfo = (String) servletRequest.getAttribute(TokenIntercepter.USER_INFO_KEY);
        JSONObject object = JSON.parseObject(userInfo);
        String uid = object.get("uid").toString();
        request.setUserId(uid);
        request.setUniqueKey(UUID.randomUUID().toString());
        CreateOrderRS response = orderCoreService.createOrder(request);
        if (response.getCode().equals(OrderRetCode.SUCCESS.getCode())) {
            return new ResponseUtil<>().setData(response.getOrderId());
        }
        return new ResponseUtil<>().setErrorMsg(response.getMsg());
    }

    /**
     * 获取当前用户的所有订单
     *
     * @return
     */
    @GetMapping("/order")
    @ApiOperation("获取当前用户的所有订单")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageInfo", value = "分页信息", dataType = "PageInfo", required = true),
            @ApiImplicitParam(name = "servletRequest", value = "HttpServletRequest", dataType = "HttpServletRequest", required = true)
    })
    public ResponseData orderByCurrentId(PageInfo pageInfo, HttpServletRequest servletRequest) {
        OrderListVO request = new OrderListVO();
        request.setPage(pageInfo.getPage());
        request.setSize(pageInfo.getSize());
        request.setSort(pageInfo.getSort());
        String userInfo = (String) servletRequest.getAttribute(TokenIntercepter.USER_INFO_KEY);
        JSONObject object = JSON.parseObject(userInfo);
        String uid = object.get("uid").toString();
        request.setUserId(uid);
        OrderListRS listResponse = orderQueryService.orderList(request);
        if (listResponse.getCode().equals(OrderRetCode.SUCCESS.getCode())) {
            PageResponse response = new PageResponse();
            response.setData(listResponse.getDetailInfoList());
            response.setTotal(listResponse.getTotal());
            return new ResponseUtil<>().setData(response);
        }
        return new ResponseUtil<>().setErrorMsg(listResponse.getMsg());
    }

    /**
     * 查询订单详情
     *
     * @return
     */
    @GetMapping("/order/{id}")
    @ApiOperation("查询订单详情")
    @ApiImplicitParam(name = "id", value = "订单ID", paramType = "path")
    public ResponseData orderDetail(@PathVariable String id) {
        OrderDetailVO request = new OrderDetailVO();
        request.setOrderId(id);
        OrderDetailRS response = orderQueryService.orderDetail(request);
        if (response.getCode().equals(OrderRetCode.SUCCESS.getCode())) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderTotal(response.getPayment());
            orderDetail.setUserId(response.getUserId());
            orderDetail.setUserName(response.getBuyerNick());
            orderDetail.setGoodsList(response.getOrderItemDto());
            orderDetail.setTel(response.getOrderShippingDto().getReceiverPhone());
            orderDetail.setStreetName(response.getOrderShippingDto().getReceiverAddress());
            return new ResponseUtil<>().setData(orderDetail);
        }
        return new ResponseUtil<>().setErrorMsg(null);
    }

    /**
     * 取消订单
     *
     * @return
     */
    @ApiOperation("取消订单")
    @PutMapping("/order/{id}")
    @ApiImplicitParam(name = "id", value = "订单ID", paramType = "path")
    public ResponseData orderCancel(@PathVariable String id) {
        CancelOrderVO request = new CancelOrderVO();
        request.setOrderId(id);
        return new ResponseUtil<>().setData(orderCoreService.cancelOrder(request));
    }

    /**
     * 删除订单
     *
     * @param id
     * @return
     */
    @ApiOperation("删除订单")
    @DeleteMapping("/order/{id}")
    @ApiImplicitParam(name = "id", value = "订单ID", paramType = "path")
    public ResponseData orderDel(@PathVariable String id) {
        DeleteOrderVO deleteOrderRequest = new DeleteOrderVO();
        deleteOrderRequest.setOrderId(id);
        return new ResponseUtil<>().setData(orderCoreService.deleteOrder(deleteOrderRequest));
    }

}
