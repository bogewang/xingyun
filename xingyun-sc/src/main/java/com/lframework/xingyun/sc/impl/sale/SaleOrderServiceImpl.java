package com.lframework.xingyun.sc.impl.sale;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.*;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.annotations.timeline.OrderTimeLineLog;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.components.security.SecurityUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.*;
import com.lframework.starter.web.inner.components.timeline.ApprovePassOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.ApproveReturnOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.CreateOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.UpdateOrderTimeLineBizType;
import com.lframework.starter.web.inner.dto.order.ApprovePassOrderDto;
import com.lframework.starter.web.inner.entity.SysUser;
import com.lframework.starter.web.inner.service.GenerateCodeService;
import com.lframework.starter.web.inner.service.system.SysUserService;
import com.lframework.xingyun.basedata.entity.*;
import com.lframework.xingyun.basedata.enums.ProductType;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.basedata.service.product.ProductBundleService;
import com.lframework.xingyun.basedata.service.product.ProductCategoryService;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.service.product.ProductUnitService;
import com.lframework.xingyun.basedata.service.storecenter.StoreCenterService;
import com.lframework.xingyun.core.utils.SplitNumberUtil;
import com.lframework.xingyun.sc.bo.sale.PrintSaleTagBo;
import com.lframework.xingyun.sc.components.code.GenerateCodeTypePool;
import com.lframework.xingyun.sc.dto.sale.SaleOrderFullDto;
import com.lframework.xingyun.sc.dto.sale.SaleOrderWithOutDto;
import com.lframework.xingyun.sc.dto.sale.SaleProductDto;
import com.lframework.xingyun.sc.entity.SaleConfig;
import com.lframework.xingyun.sc.entity.SaleOrder;
import com.lframework.xingyun.sc.entity.SaleOrderDetail;
import com.lframework.xingyun.sc.entity.SaleOrderDetailBundle;
import com.lframework.xingyun.sc.enums.SaleOpLogType;
import com.lframework.xingyun.sc.enums.SaleOrderStatus;
import com.lframework.xingyun.sc.events.order.impl.ApprovePassSaleOrderEvent;
import com.lframework.xingyun.sc.excel.sale.SaleOrderImportModel;
import com.lframework.xingyun.sc.mappers.SaleOrderMapper;
import com.lframework.xingyun.sc.service.paytype.OrderPayTypeService;
import com.lframework.xingyun.sc.service.sale.SaleConfigService;
import com.lframework.xingyun.sc.service.sale.SaleOrderDetailBundleService;
import com.lframework.xingyun.sc.service.sale.SaleOrderDetailService;
import com.lframework.xingyun.sc.service.sale.SaleOrderService;
import com.lframework.xingyun.sc.vo.sale.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SaleOrderServiceImpl extends BaseMpServiceImpl<SaleOrderMapper, SaleOrder> implements
        SaleOrderService {

  private List<String> NO_NEED_PRINT = Lists.newArrayList("调料干杂");

  @Autowired
  private SaleOrderDetailService saleOrderDetailService;

  @Autowired
  private SaleOrderDetailBundleService saleOrderDetailBundleService;

  @Autowired
  private ProductBundleService productBundleService;

  @Autowired
  private GenerateCodeService generateCodeService;

  @Autowired
  private StoreCenterService storeCenterService;

  @Autowired
  private CustomerService customerService;

  @Autowired
  private SysUserService userService;

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductUnitService productUnitService;

  @Autowired
  private SaleConfigService saleConfigService;

  @Autowired
  private OrderPayTypeService orderPayTypeService;
  @Autowired
  private ProductCategoryService productCategoryService;

  @Override
  public PageResult<SaleOrder> query(Integer pageIndex, Integer pageSize, QuerySaleOrderVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<SaleOrder> datas = this.query(vo);

    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  @Override
  public List<SaleOrder> query(QuerySaleOrderVo vo) {

    return getBaseMapper().query(vo);
  }

  @Override
  public PageResult<SaleOrder> selector(Integer pageIndex, Integer pageSize,
                                        SaleOrderSelectorVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<SaleOrder> datas = getBaseMapper().selector(vo);

    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  @Override
  public SaleOrderFullDto getDetail(String id) {

    SaleOrderFullDto order = getBaseMapper().getDetail(id);
    if (order == null) {
      throw new InputErrorException("订单不存在！");
    }

    return order;
  }

  @Override
  public SaleOrderWithOutDto getWithOut(String id) {

    SaleConfig saleConfig = saleConfigService.get();

    SaleOrderWithOutDto order = getBaseMapper().getWithOut(id, saleConfig.getOutStockRequireSale());
    if (order == null) {
      throw new InputErrorException("订单不存在！");
    }
    return order;
  }

  @Override
  public PageResult<SaleOrder> queryWithOut(Integer pageIndex, Integer pageSize,
                                            QuerySaleOrderWithOutVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    SaleConfig saleConfig = saleConfigService.get();

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<SaleOrder> datas = getBaseMapper().queryWithOut(vo,
            saleConfig.getOutStockMultipleRelateSale());

    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  @OpLog(type = SaleOpLogType.class, name = "创建销售订单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = CreateOrderTimeLineBizType.class, orderId = "#_result", name = "创建订单")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public String create(CreateSaleOrderVo vo) {

    SaleOrder order = new SaleOrder();
    order.setId(IdUtil.getId());
    order.setCode(generateCodeService.generate(GenerateCodeTypePool.SALE_ORDER));

    this.create(order, vo);

    order.setStatus(SaleOrderStatus.CREATED);

    getBaseMapper().insert(order);

    OpLogUtil.setVariable("code", order.getCode());
    OpLogUtil.setExtra(vo);

    return order.getId();
  }

  @OpLog(type = SaleOpLogType.class, name = "修改销售订单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = UpdateOrderTimeLineBizType.class, orderId = "#vo.id", name = "修改订单")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void update(UpdateSaleOrderVo vo) {

    SaleOrder order = getBaseMapper().selectById(vo.getId());
    if (order == null) {
      throw new InputErrorException("订单不存在！");
    }

    if (order.getStatus() != SaleOrderStatus.CREATED
            && order.getStatus() != SaleOrderStatus.APPROVE_REFUSE) {

      if (order.getStatus() == SaleOrderStatus.APPROVE_PASS) {
        throw new DefaultClientException("订单已审核通过，无法修改！");
      }

      throw new DefaultClientException("订单无法修改！");
    }

    // 删除订单明细
    Wrapper<SaleOrderDetail> deleteDetailWrapper = Wrappers.lambdaQuery(SaleOrderDetail.class)
            .eq(SaleOrderDetail::getOrderId, order.getId());
    saleOrderDetailService.remove(deleteDetailWrapper);

    // 删除组合商品信息
    Wrapper<SaleOrderDetailBundle> deleteDetailBundleWrapper = Wrappers.lambdaQuery(
            SaleOrderDetailBundle.class).eq(SaleOrderDetailBundle::getOrderId, order.getId());
    saleOrderDetailBundleService.remove(deleteDetailBundleWrapper);

    this.create(order, vo);

    order.setStatus(SaleOrderStatus.CREATED);

    List<SaleOrderStatus> statusList = new ArrayList<>();
    statusList.add(SaleOrderStatus.CREATED);
    statusList.add(SaleOrderStatus.APPROVE_REFUSE);

    Wrapper<SaleOrder> updateOrderWrapper = Wrappers.lambdaUpdate(SaleOrder.class)
            .set(SaleOrder::getApproveBy, null).set(SaleOrder::getApproveTime, null)
            .set(SaleOrder::getRefuseReason, StringPool.EMPTY_STR).eq(SaleOrder::getId, order.getId())
            .in(SaleOrder::getStatus, statusList);
    if (getBaseMapper().updateAllColumn(order, updateOrderWrapper) != 1) {
      throw new DefaultClientException("订单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", order.getCode());
    OpLogUtil.setExtra(vo);
  }

  @OpLog(type = SaleOpLogType.class, name = "审核通过销售订单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核通过")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void approvePass(ApprovePassSaleOrderVo vo) {

    SaleOrder order = getBaseMapper().selectById(vo.getId());
    if (order == null) {
      throw new InputErrorException("订单不存在！");
    }

    if (order.getStatus() != SaleOrderStatus.CREATED && order.getStatus() != SaleOrderStatus.APPROVE_REFUSE) {

      if (order.getStatus() == SaleOrderStatus.APPROVE_PASS) {
        throw new DefaultClientException("订单已审核通过，不允许继续执行审核！");
      }

      throw new DefaultClientException("订单无法审核通过！");
    }

    order.setStatus(SaleOrderStatus.APPROVE_PASS);

    List<SaleOrderStatus> statusList = new ArrayList<>();
    statusList.add(SaleOrderStatus.CREATED);
    statusList.add(SaleOrderStatus.APPROVE_REFUSE);

    LambdaUpdateWrapper<SaleOrder> updateOrderWrapper = Wrappers.lambdaUpdate(SaleOrder.class)
            .set(SaleOrder::getApproveBy, SecurityUtil.getCurrentUser().getId())
            .set(SaleOrder::getApproveTime, LocalDateTime.now()).eq(SaleOrder::getId, order.getId())
            .in(SaleOrder::getStatus, statusList);
    if (!StringUtil.isBlank(vo.getDescription())) {
      updateOrderWrapper.set(SaleOrder::getDescription, vo.getDescription());
    }
    if (getBaseMapper().updateAllColumn(order, updateOrderWrapper) != 1) {
      throw new DefaultClientException("订单信息已过期，请刷新重试！");
    }

    Wrapper<SaleOrderDetail> queryDetailWrapper = Wrappers.lambdaQuery(SaleOrderDetail.class)
            .eq(SaleOrderDetail::getOrderId, order.getId()).orderByAsc(SaleOrderDetail::getOrderNo);
    List<SaleOrderDetail> details = saleOrderDetailService.list(queryDetailWrapper);

    BigDecimal totalNum = BigDecimal.ZERO;
    BigDecimal giftNum = BigDecimal.ZERO;
    BigDecimal totalAmount = BigDecimal.ZERO;

    for (SaleOrderDetail detail : details) {
      boolean isGift = detail.getIsGift();
      totalAmount = NumberUtil.add(totalAmount,
              NumberUtil.getNumber(NumberUtil.mul(detail.getTaxPrice(), detail.getOrderNum()), 2));

      Product product = productService.findById(detail.getProductId());
      if (product.getProductType() == ProductType.NORMAL) {
        if (isGift) {
          giftNum = NumberUtil.add(giftNum, detail.getOrderNum());
        } else {
          totalNum = NumberUtil.add(totalNum, detail.getOrderNum());
        }
      } else {
        Wrapper<SaleOrderDetailBundle> queryBundleWrapper = Wrappers.lambdaQuery(
                        SaleOrderDetailBundle.class).eq(SaleOrderDetailBundle::getOrderId, order.getId())
                .eq(SaleOrderDetailBundle::getDetailId, detail.getId());
        List<SaleOrderDetailBundle> saleOrderDetailBundles = saleOrderDetailBundleService.list(
                queryBundleWrapper);
        Assert.notEmpty(saleOrderDetailBundles);

        for (SaleOrderDetailBundle saleOrderDetailBundle : saleOrderDetailBundles) {
          SaleOrderDetail newDetail = new SaleOrderDetail();
          newDetail.setId(IdUtil.getId());
          newDetail.setOrderId(order.getId());
          newDetail.setProductId(saleOrderDetailBundle.getProductId());
          newDetail.setOrderNum(saleOrderDetailBundle.getProductOrderNum());
          newDetail.setOriPrice(saleOrderDetailBundle.getProductOriPrice());
          newDetail.setTaxPrice(saleOrderDetailBundle.getProductTaxPrice());
          newDetail.setDiscountRate(detail.getDiscountRate());
          newDetail.setIsGift(detail.getIsGift());
          newDetail.setTaxRate(saleOrderDetailBundle.getProductTaxRate());
          newDetail.setDescription(detail.getDescription());
          newDetail.setOrderNo(detail.getOrderNo());
          newDetail.setOriBundleDetailId(detail.getId());
          newDetail.setTaxAmount(saleOrderDetailBundle.getProductTaxAmount());

          saleOrderDetailService.save(newDetail);
          saleOrderDetailService.removeById(detail.getId());

          saleOrderDetailBundle.setProductDetailId(newDetail.getId());
          saleOrderDetailBundleService.updateById(saleOrderDetailBundle);

          if (isGift) {
            giftNum = NumberUtil.add(giftNum, newDetail.getOrderNum());
          } else {
            totalNum = NumberUtil.add(totalNum, newDetail.getOrderNum());
          }
        }
      }
    }

    // 这里需要重新统计明细信息，因为明细发生变动了
    Wrapper<SaleOrder> updateWrapper = Wrappers.lambdaUpdate(SaleOrder.class)
            .set(SaleOrder::getTotalNum, totalNum).set(SaleOrder::getTotalGiftNum, giftNum)
            .set(SaleOrder::getTotalAmount, totalAmount).eq(SaleOrder::getId, order.getId());
    this.update(updateWrapper);

    OpLogUtil.setVariable("code", order.getCode());
    OpLogUtil.setExtra(vo);

    this.sendApprovePassEvent(order);
  }

  @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#_result", name = "直接审核通过")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public String directApprovePass(CreateSaleOrderVo vo) {

    SaleOrderService thisService = getThis(this.getClass());

    String orderId = thisService.create(vo);

    ApprovePassSaleOrderVo approvePassSaleOrderVo = new ApprovePassSaleOrderVo();
    approvePassSaleOrderVo.setId(orderId);
    approvePassSaleOrderVo.setDescription(vo.getDescription());

    thisService.approvePass(approvePassSaleOrderVo);

    return orderId;
  }

  @OpLog(type = SaleOpLogType.class, name = "审核拒绝销售订单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = ApproveReturnOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核拒绝，拒绝理由：{}", params = "#vo.refuseReason")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void approveRefuse(ApproveRefuseSaleOrderVo vo) {

    SaleOrder order = getBaseMapper().selectById(vo.getId());
    if (order == null) {
      throw new InputErrorException("订单不存在！");
    }

    if (order.getStatus() != SaleOrderStatus.CREATED) {

      if (order.getStatus() == SaleOrderStatus.APPROVE_PASS) {
        throw new DefaultClientException("订单已审核通过，不允许继续执行审核！");
      }

      if (order.getStatus() == SaleOrderStatus.APPROVE_REFUSE) {
        throw new DefaultClientException("订单已审核拒绝，不允许继续执行审核！");
      }

      throw new DefaultClientException("订单无法审核拒绝！");
    }

    order.setStatus(SaleOrderStatus.APPROVE_REFUSE);

    LambdaUpdateWrapper<SaleOrder> updateOrderWrapper = Wrappers.lambdaUpdate(SaleOrder.class)
            .set(SaleOrder::getApproveBy, SecurityUtil.getCurrentUser().getId())
            .set(SaleOrder::getApproveTime, LocalDateTime.now())
            .set(SaleOrder::getRefuseReason, vo.getRefuseReason()).eq(SaleOrder::getId, order.getId())
            .eq(SaleOrder::getStatus, SaleOrderStatus.CREATED);
    if (getBaseMapper().updateAllColumn(order, updateOrderWrapper) != 1) {
      throw new DefaultClientException("订单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", order.getCode());
    OpLogUtil.setExtra(vo);
  }

  @OpLog(type = SaleOpLogType.class, name = "删除销售订单，单号：{}", params = "#code")
  @OrderTimeLineLog(orderId = "#id", delete = true)
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void deleteById(String id) {

    Assert.notBlank(id);
    SaleOrder order = getBaseMapper().selectById(id);
    if (order == null) {
      throw new InputErrorException("订单不存在！");
    }

    if (order.getStatus() != SaleOrderStatus.CREATED
            && order.getStatus() != SaleOrderStatus.APPROVE_REFUSE) {

      if (order.getStatus() == SaleOrderStatus.APPROVE_PASS) {
        throw new DefaultClientException("“审核通过”的销售单据不允许执行删除操作！");
      }

      throw new DefaultClientException("订单无法删除！");
    }

    // 删除订单明细
    Wrapper<SaleOrderDetail> deleteDetailWrapper = Wrappers.lambdaQuery(SaleOrderDetail.class)
            .eq(SaleOrderDetail::getOrderId, order.getId());
    saleOrderDetailService.remove(deleteDetailWrapper);

    // 删除组合商品信息
    Wrapper<SaleOrderDetailBundle> deleteDetailBundleWrapper = Wrappers.lambdaQuery(
            SaleOrderDetailBundle.class).eq(SaleOrderDetailBundle::getOrderId, order.getId());
    saleOrderDetailBundleService.remove(deleteDetailBundleWrapper);

    // 删除订单
    Wrapper<SaleOrder> deleteWrapper = Wrappers.lambdaQuery(SaleOrder.class)
            .eq(SaleOrder::getId, id)
            .in(SaleOrder::getStatus, SaleOrderStatus.CREATED, SaleOrderStatus.APPROVE_REFUSE);
    if (!remove(deleteWrapper)) {
      throw new DefaultClientException("订单信息已过期，请刷新重试！");
    }

    orderPayTypeService.deleteByOrderId(id);

    OpLogUtil.setVariable("code", order.getCode());
  }

  @Override
  public PageResult<SaleProductDto> querySaleByCondition(Integer pageIndex, Integer pageSize,
                                                         String scId, String condition, Boolean isReturn) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    PageHelperUtil.startPage(pageIndex, pageSize);

    List<SaleProductDto> datas = getBaseMapper().querySaleByCondition(scId, condition, isReturn);
    fillProductUnits(datas);
    PageResult<SaleProductDto> pageResult = PageResultUtil.convert(new PageInfo<>(datas));

    return pageResult;
  }

  @Override
  public PageResult<SaleProductDto> querySaleList(Integer pageIndex, Integer pageSize,
                                                  QuerySaleProductVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    PageHelperUtil.startPage(pageIndex, pageSize);

    List<SaleProductDto> datas = getBaseMapper().querySaleList(vo);
    fillProductUnits(datas);
    PageResult<SaleProductDto> pageResult = PageResultUtil.convert(new PageInfo<>(datas));

    return pageResult;
  }

  @Override
  public SaleProductDto getSaleById(String id) {

    SaleProductDto data = getBaseMapper().getSaleById(id);
    if (data != null) {
      data.setUnits(productUnitService.getByProductId(data.getId()));
    }

    return data;
  }

  private void fillProductUnits(List<SaleProductDto> datas) {
    datas.forEach(data -> data.setUnits(productUnitService.getByProductId(data.getId())));
  }

  private void create(SaleOrder order, CreateSaleOrderVo vo) {

    StoreCenter sc = storeCenterService.findById(vo.getScId());
    if (sc == null) {
      throw new InputErrorException("仓库不存在！");
    }

    order.setScId(vo.getScId());

    Customer customer = customerService.findById(vo.getCustomerId());
    if (customer == null) {
      throw new InputErrorException("客户不存在！");
    }
    order.setCustomerId(vo.getCustomerId());

    if (!StringUtil.isBlank(vo.getSalerId())) {
      SysUser saler = userService.findById(vo.getSalerId());
      if (saler == null) {
        throw new InputErrorException("销售员不存在！");
      }

      order.setSalerId(vo.getSalerId());
    }

    order.setOrderDate(vo.getOrderDate());

    BigDecimal totalNum = BigDecimal.ZERO;
    BigDecimal giftNum = BigDecimal.ZERO;
    BigDecimal totalAmount = BigDecimal.ZERO;
    int orderNo = 1;
    for (SaleProductVo productVo : vo.getProducts()) {

      boolean isGift = productVo.getTaxPrice().doubleValue() == 0D;

      if (isGift) {
        giftNum = NumberUtil.add(giftNum, productVo.getOrderNum());
      } else {
        totalNum = NumberUtil.add(totalNum, productVo.getOrderNum());
      }

      totalAmount = NumberUtil.add(totalAmount,
              NumberUtil.getNumber(NumberUtil.mul(productVo.getTaxPrice(), productVo.getOrderNum()),
                      2));

      SaleOrderDetail orderDetail = new SaleOrderDetail();
      orderDetail.setId(IdUtil.getId());
      orderDetail.setOrderId(order.getId());

      Product product = productService.findById(productVo.getProductId());
      if (product == null) {
        throw new InputErrorException("第" + orderNo + "行商品不存在！");
      }

      orderDetail.setProductId(productVo.getProductId());
      orderDetail.setOrderNum(productVo.getOrderNum());
      orderDetail.setOriPrice(productVo.getOriPrice());
      orderDetail.setDiscountRate(productVo.getDiscountRate());
      orderDetail.setTaxPrice(productVo.getTaxPrice());
      orderDetail.setIsGift(isGift);
      orderDetail.setTaxRate(product.getSaleTaxRate());
      orderDetail.setDescription(
              StringUtil.isBlank(productVo.getDescription()) ? StringPool.EMPTY_STR
                      : productVo.getDescription());
      orderDetail.setOrderNo(orderNo);
      orderDetail.setTaxAmount(
              NumberUtil.getNumber(NumberUtil.mul(orderDetail.getTaxPrice(), orderDetail.getOrderNum()),
                      2));

      saleOrderDetailService.save(orderDetail);

      // 这里处理组合商品
      if (product.getProductType() == ProductType.BUNDLE) {
        if (!NumberUtil.isInteger(productVo.getOrderNum())) {
          throw new InputErrorException("第" + orderNo + "行商品销售数量必须是整数！");
        }
        List<ProductBundle> productBundles = productBundleService.getByMainProductId(
                product.getId());
        // 构建指标项
        Map<Object, Number> bundleWeight = new HashMap<>(productBundles.size());
        for (ProductBundle productBundle : productBundles) {
          bundleWeight.put(productBundle.getProductId(),
                  NumberUtil.mul(productBundle.getSalePrice(), productBundle.getBundleNum()));
        }
        Map<Object, Number> splitPriceMap = SplitNumberUtil.split(orderDetail.getTaxAmount(),
                bundleWeight, 2);
        List<SaleOrderDetailBundle> saleOrderDetailBundles = productBundles.stream()
                .map(productBundle -> {
                  Product bundle = productService.findById(productBundle.getProductId());
                  SaleOrderDetailBundle saleOrderDetailBundle = new SaleOrderDetailBundle();
                  saleOrderDetailBundle.setId(IdUtil.getId());
                  saleOrderDetailBundle.setOrderId(order.getId());
                  saleOrderDetailBundle.setDetailId(orderDetail.getId());
                  saleOrderDetailBundle.setMainProductId(product.getId());
                  saleOrderDetailBundle.setOrderNum(orderDetail.getOrderNum());
                  saleOrderDetailBundle.setProductId(productBundle.getProductId());
                  saleOrderDetailBundle.setProductOrderNum(
                          NumberUtil.mul(orderDetail.getOrderNum(), productBundle.getBundleNum()));
                  saleOrderDetailBundle.setProductOriPrice(productBundle.getSalePrice());
                  saleOrderDetailBundle.setProductTaxAmount(BigDecimal.valueOf(
                          splitPriceMap.get(productBundle.getProductId()).doubleValue()));
                  // 这里会有尾差
                  saleOrderDetailBundle.setProductTaxPrice(NumberUtil.getNumber(NumberUtil.div(
                          saleOrderDetailBundle.getProductTaxAmount(),
                          saleOrderDetailBundle.getProductOrderNum()), 6));
                  saleOrderDetailBundle.setProductTaxRate(bundle.getSaleTaxRate());

                  return saleOrderDetailBundle;
                }).collect(Collectors.toList());

        saleOrderDetailBundleService.saveBatch(saleOrderDetailBundles);
      }
      orderNo++;
    }
    order.setTotalNum(totalNum);
    order.setTotalGiftNum(giftNum);
    order.setTotalAmount(totalAmount);
    order.setDescription(
            StringUtil.isBlank(vo.getDescription()) ? StringPool.EMPTY_STR : vo.getDescription());

    orderPayTypeService.create(order.getId(), vo.getPayTypes());
  }

  private void sendApprovePassEvent(SaleOrder order) {

    ApprovePassOrderDto dto = new ApprovePassOrderDto();
    dto.setId(order.getId());
    dto.setTotalAmount(order.getTotalAmount());
    dto.setApproveTime(order.getApproveTime());

    ApprovePassSaleOrderEvent event = new ApprovePassSaleOrderEvent(this, dto);

    ApplicationUtil.publishEvent(event);
  }

  @Override
  public List<SaleProductVo> checkImport(List<SaleOrderImportModel> list) {
    if (CollectionUtils.isEmpty(list)) {
      return Lists.newArrayList();
    }

    // 匹配编号
    checkImportData(list);

    return list.stream()
            .map(item -> BeanUtil.copyProperties(item, SaleProductVo.class))
            .collect(Collectors.toList());
  }

  private void checkImportData(List<SaleOrderImportModel> list) {
    List<String> productNames = list.stream().map(SaleOrderImportModel::getProductName)
            .collect(Collectors.toList());
    List<Product> products = productService.selectByProductName(productNames);
    Map<String, Product> nameSpecUnitMap = products.stream()
            .collect(Collectors.toMap(item -> item.getName() + item.getSpec() + item.getUnit(), item -> item));
    Map<String, Product> nameUnitMap = products.stream()
            .collect(Collectors.toMap(item -> item.getName() + item.getUnit(), item -> item));

    for (int i = 0; i < list.size(); i++) {
      SaleOrderImportModel data = list.get(i);
      int rowIndex = i + 2;

      if (StringUtils.isEmpty(data.getProductName())) {
        throw new DefaultClientException("第" + rowIndex + "行“商品名称”不能为空");
      }
      if (StringUtils.isEmpty(data.getUnit())) {
        throw new DefaultClientException("第" + rowIndex + "行“单位”不能为空");
      }
      if (data.getTaxPrice() == null) {
        throw new DefaultClientException("第" + rowIndex + "行“单价”不能为空");
      }
      if (data.getOrderNum() == null) {
        throw new DefaultClientException("第" + rowIndex + "行“数量”不能为空");
      }
      if (NumberUtil.le(data.getOrderNum(), BigDecimal.ZERO)) {
        throw new DefaultClientException("第" + rowIndex + "行“数量”必须大于0");
      }
      if (!NumberUtil.isNumberPrecision(data.getOrderNum(), 8)) {
        throw new DefaultClientException("第" + rowIndex + "行“数量”最多允许8位小数");
      }

      // 匹配商品,设置商品编号
      String spec = data.getSpec() == null ? StringPool.EMPTY_STR : data.getSpec();
      String nameSpecUnit = data.getProductName() + spec + data.getUnit();
      Product product = nameSpecUnitMap.get(nameSpecUnit);
      if (product == null) {
        product = nameUnitMap.get(nameSpecUnit);
        if (product == null) {
          throw new DefaultClientException("第" + rowIndex + "行“商品名称”、“规格”、“单位”组合不存在");
        }
      }
      data.setProductCode(product.getCode());
      data.setProductId(product.getId());
    }
  }

  @Override
  public List<PrintSaleTagBo> tagPrint(QuerySaleOrderVo vo) {
    PageResult<SaleOrder> result = this.query(1, Integer.MAX_VALUE, vo);
    if (CollectionUtils.isEmpty(result.getDatas())) {
      return Lists.newArrayList();
    }
    // 调料不需要打印标签
    List<String> noNeedPrint = productCategoryService.getAllProductCategories().stream()
            .filter(item -> NO_NEED_PRINT.contains(item.getName()))
            .map(ProductCategory::getId)
            .collect(Collectors.toList());

    List<PrintSaleTagBo> res = Lists.newArrayList();
    result.getDatas().forEach(item -> {
      Customer customer = customerService.findById(item.getCustomerId());
      List<SaleOrderDetail> details = saleOrderDetailService.getByOrderIds(Lists.newArrayList(item.getId()));

      List<String> productIds = details.stream()
              .map(SaleOrderDetail::getProductId)
              .collect(Collectors.toList());
      // 过滤掉某些不需要汇总的商品，比如调料， todo可以配置在数据库中
      Map<String, Product> productMap = productService.getBaseMapper().selectBatchIds(productIds).stream()
              .filter(product -> !noNeedPrint.contains(product.getCategoryId()))
              .collect(Collectors.toMap(Product::getId, r -> r, (v1, v2) -> v2));
      // 组装成打印数据；
      // 按商品汇总
      Map<String, List<SaleOrderDetail>> map = details.stream()
              .filter(detail -> productMap.containsKey(detail.getProductId()))
              .collect(Collectors.groupingBy(SaleOrderDetail::getProductId));

      List<PrintSaleTagBo> collect = map.keySet().stream()
              .map(productId -> {
                PrintSaleTagBo bo = new PrintSaleTagBo();
                bo.setCustomerSimpleName(customer.getNickName());

                bo.setProductName(productMap.get(productId).getName());
                List<SaleOrderDetail> orderDetails = map.get(productId);
                BigDecimal orderNum = orderDetails.stream().map(SaleOrderDetail::getOrderNum).reduce(BigDecimal.ZERO, BigDecimal::add);
                // 保留1位小数
                String format = orderNum.setScale(1, RoundingMode.HALF_UP).toString();
                bo.setOrderNum(String.format("%s%s", format, productMap.get(productId).getUnit()));
                bo.setOrderDate(item.getOrderDate().toString());

                return bo;
              }).collect(Collectors.toList());
      if (CollectionUtils.isEmpty(collect)) {
        return;
      }
      res.addAll(collect);
    });
    return res;
  }
}
