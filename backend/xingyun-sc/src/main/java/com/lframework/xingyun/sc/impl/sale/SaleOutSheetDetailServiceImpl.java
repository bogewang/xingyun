package com.lframework.xingyun.sc.impl.sale;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import com.lframework.xingyun.sc.mappers.SaleOutSheetDetailMapper;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SaleOutSheetDetailServiceImpl extends
    BaseMpServiceImpl<SaleOutSheetDetailMapper, SaleOutSheetDetail>
    implements SaleOutSheetDetailService {

  @Autowired
  private ProductService productService;

  @Override
  public List<SaleOutSheetDetail> getBySheetId(String sheetId) {
      LambdaQueryWrapper<SaleOutSheetDetail> wrapper = Wrappers.lambdaQuery(SaleOutSheetDetail.class)
              .eq(SaleOutSheetDetail::getSheetId, sheetId);
      return getBaseMapper().selectList(wrapper);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void addReturnNum(String id, BigDecimal num) {

    Assert.notBlank(id);
    Assert.greaterThanZero(num);

    SaleOutSheetDetail detail = getBaseMapper().selectById(id);

    BigDecimal remainNum = NumberUtil.sub(detail.getOrderNum(), detail.getReturnNum());
    if (NumberUtil.lt(remainNum, num)) {
      Product product = productService.findById(detail.getProductId());

      throw new DefaultClientException(
          "（" + product.getCode() + "）" + product.getName() + "剩余退货数量为" + remainNum
              + "个，本次退货数量不允许大于"
              + remainNum + "个！");
    }

    if (getBaseMapper().addReturnNum(detail.getId(), num) != 1) {
      Product product = productService.findById(detail.getProductId());

      throw new DefaultClientException(
          "（" + product.getCode() + "）" + product.getName() + "剩余退货数量不足，不允许继续退货！");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void subReturnNum(String id, BigDecimal num) {

    Assert.notBlank(id);
    Assert.greaterThanZero(num);

    SaleOutSheetDetail orderDetail = getBaseMapper().selectById(id);

    if (NumberUtil.lt(orderDetail.getReturnNum(), num)) {
      Product product = productService.findById(orderDetail.getProductId());

      throw new DefaultClientException(
          "（" + product.getCode() + "）" + product.getName() + "已退货数量为"
              + orderDetail.getReturnNum()
              + "个，本次取消退货数量不允许大于" + orderDetail.getReturnNum() + "个！");
    }

    if (getBaseMapper().subReturnNum(orderDetail.getId(), num) != 1) {
      Product product = productService.findById(orderDetail.getProductId());

      throw new DefaultClientException(
          "（" + product.getCode() + "）" + product.getName() + "已退货数量不足，不允许取消退货！");
    }
  }

  @Override
  public BigDecimal getTotalWeightBySheetIds(List<String> sheetIds) {
    if (CollectionUtil.isEmpty(sheetIds)) {
      return BigDecimal.ZERO;
    }

    Wrapper<SaleOutSheetDetail> queryWrapper = Wrappers.lambdaQuery(SaleOutSheetDetail.class)
        .in(SaleOutSheetDetail::getSheetId, sheetIds);
    List<SaleOutSheetDetail> details = this.list(queryWrapper);
    BigDecimal sumWeight = details.stream().map(t -> {
      Product product = productService.findById(t.getProductId());
      if (product.getWeight() == null) {
        throw new DefaultClientException(
            "商品（" + product.getCode() + "）" + product.getName() + "尚未设置重量，请检查！");
      }
      return NumberUtil.getNumber(NumberUtil.mul(t.getOrderNum(), product.getWeight()), 2);
    }).reduce(NumberUtil::add).orElse(BigDecimal.ZERO);

    return sumWeight;
  }

  @Override
  public BigDecimal getTotalVolumeBySheetIds(List<String> sheetIds) {
    if (CollectionUtil.isEmpty(sheetIds)) {
      return BigDecimal.ZERO;
    }

    Wrapper<SaleOutSheetDetail> queryWrapper = Wrappers.lambdaQuery(SaleOutSheetDetail.class)
        .in(SaleOutSheetDetail::getSheetId, sheetIds);
    List<SaleOutSheetDetail> details = this.list(queryWrapper);
    BigDecimal sumVolume = details.stream().map(t -> {
      Product product = productService.findById(t.getProductId());
      if (product.getVolume() == null) {
        throw new DefaultClientException(
            "商品（" + product.getCode() + "）" + product.getName() + "尚未设置体积，请检查！");
      }
      return NumberUtil.getNumber(NumberUtil.mul(t.getOrderNum(), product.getVolume()), 2);
    }).reduce(NumberUtil::add).orElse(BigDecimal.ZERO);

    return sumVolume;
  }
}
