package com.lframework.xingyun.sc.impl.stock;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.exceptions.impl.DefaultSysException;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.PageHelperUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.service.product.ProductLatestPriceCacheService;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.sc.dto.stock.ProductStockChangeDto;
import com.lframework.xingyun.sc.entity.ProductStock;
import com.lframework.xingyun.sc.enums.ProductStockBizType;
import com.lframework.xingyun.sc.events.stock.AddStockEvent;
import com.lframework.xingyun.sc.events.stock.SubStockEvent;
import com.lframework.xingyun.sc.mappers.ProductStockMapper;
import com.lframework.xingyun.sc.service.stock.ProductStockLogService;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import com.lframework.xingyun.sc.vo.stock.AddProductStockVo;
import com.lframework.xingyun.sc.vo.stock.QueryProductStockVo;
import com.lframework.xingyun.sc.vo.stock.SubProductStockVo;
import com.lframework.xingyun.sc.vo.stock.log.AddLogWithAddStockVo;
import com.lframework.xingyun.sc.vo.stock.log.AddLogWithSubStockVo;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductStockServiceImpl extends BaseMpServiceImpl<ProductStockMapper, ProductStock>
        implements ProductStockService {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductStockLogService productStockLogService;

    @Resource
    private ProductLatestPriceCacheService productLatestPriceCacheService;

    @Override
    public PageResult<ProductStock> query(Integer pageIndex, Integer pageSize,
            QueryProductStockVo vo) {

        Assert.greaterThanZero(pageIndex);
        Assert.greaterThanZero(pageSize);

        PageHelperUtil.startPage(pageIndex, pageSize);
        List<ProductStock> datas = this.query(vo);

        return PageResultUtil.convert(new PageInfo<>(datas));
    }

    @Override
    public List<ProductStock> query(QueryProductStockVo vo) {

        return getBaseMapper().query(vo);
    }

    @Override
    public ProductStock getByProductIdAndScId(String productId, String scId) {

        Product product = productService.findById(productId);
        if (product == null) {
            return null;
        }

        return getBaseMapper().getByProductIdAndScId(productId, scId);
    }

    @Override
    public List<ProductStock> getByProductIdsAndScId(List<String> productIds, String scId) {

        if (CollectionUtil.isEmpty(productIds)) {
            return CollectionUtil.emptyList();
        }

        return getBaseMapper().getByProductIdsAndScId(productIds, scId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ProductStockChangeDto addStock(AddProductStockVo vo) {

        Assert.greaterThanZero(vo.getStockNum());

        Product product = productService.findById(vo.getProductId());
        ProductStock productStock = getProductStock(vo.getProductId(), vo.getScId());

        boolean isStockEmpty = false;
        if (productStock == null) {
            // 首次入库，先新增
            productStock = generateProductStock(vo.getProductId(), vo.getScId());
            getBaseMapper().insert(productStock);
            isStockEmpty = true;
        }

        // 如果taxAmount为null，代表不重算均价，即：按当前均价直接入库
        boolean reCalcCostPrice = vo.getTaxAmount() != null;

        if (vo.getTaxAmount() == null) {
            vo.setTaxAmount(isStockEmpty ? vo.getDefaultTaxAmount() : productStock.getTaxAmount());
        }
        if (vo.getTaxAmount() == null) {
            // 如果此时taxPrice还是null，则代表taxPrice和defaultTaxPrice均为null
            throw new DefaultSysException(
                    "商品ID：" + vo.getProductId() + "，没有库存，taxAmount和defaultTaxAmount不能同时为null！");
        }

        if (isStockEmpty) {
            // 如果之前没有库存，那么均价必须重算
            reCalcCostPrice = true;
        }

        vo.setTaxAmount(NumberUtil.getNumber(vo.getTaxAmount(), 2));
        int count = getBaseMapper().addStock(vo.getProductId(), vo.getScId(), vo.getStockNum(),
                vo.getTaxAmount(), productStock.getStockNum(),
                productStock.getTaxAmount(),
                vo.getTaxPrice(),
                reCalcCostPrice);
        if (count != 1) {
            throw new DefaultClientException(
                    "商品（" + product.getCode() + "）" + product.getName() + "入库失败，请稍后重试！");
        }

        AddLogWithAddStockVo addLogWithAddStockVo = new AddLogWithAddStockVo();
        addLogWithAddStockVo.setProductId(vo.getProductId());
        addLogWithAddStockVo.setScId(vo.getScId());
        addLogWithAddStockVo.setStockNum(vo.getStockNum());
        addLogWithAddStockVo
                .setTaxAmount(vo.getLogTaxAmount() == null ? vo.getTaxAmount() : vo.getLogTaxAmount());
        addLogWithAddStockVo.setOriStockNum(productStock.getStockNum());
        addLogWithAddStockVo.setCurStockNum(
                NumberUtil.add(productStock.getStockNum(), vo.getStockNum()));
        addLogWithAddStockVo.setOriTaxPrice(productStock.getTaxPrice());
        addLogWithAddStockVo.setCurTaxPrice(calcAddStockCurTaxPrice(reCalcCostPrice,
                addLogWithAddStockVo.getCurStockNum(),
                productStock.getTaxPrice(),
                vo.getTaxPrice(),
                productStock.getTaxAmount(),
                vo.getTaxAmount()));
        addLogWithAddStockVo.setCreateTime(vo.getCreateTime());
        addLogWithAddStockVo.setBizId(vo.getBizId());
        addLogWithAddStockVo.setBizDetailId(vo.getBizDetailId());
        addLogWithAddStockVo.setBizCode(vo.getBizCode());
        addLogWithAddStockVo.setBizType(vo.getBizType());

        productStockLogService.addLogWithAddStock(addLogWithAddStockVo);
        ProductStockChangeDto stockChange = new ProductStockChangeDto();
        stockChange.setScId(vo.getScId());
        stockChange.setProductId(vo.getProductId());
        stockChange.setNum(vo.getStockNum());
        stockChange.setTaxAmount(vo.getTaxAmount());
        stockChange.setCurTaxPrice(addLogWithAddStockVo.getCurTaxPrice());
        stockChange.setCreateTime(vo.getCreateTime());
        stockChange.setCurStockNum(addLogWithAddStockVo.getCurStockNum());

        AddStockEvent addStockEvent = new AddStockEvent(this, stockChange);
        ApplicationUtil.publishEvent(addStockEvent);

        return stockChange;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ProductStockChangeDto subStock(SubProductStockVo vo) {
        Assert.greaterThanZero(vo.getStockNum());

        Product product = productService.findById(vo.getProductId());

        ProductStock productStock = getProductStock(vo.getProductId(), vo.getScId());
        if (productStock == null) {
            productStock = generateProductStock(vo.getProductId(), vo.getScId());
            getBaseMapper().insert(productStock);
        }

        BigDecimal curStockNum = NumberUtil.sub(productStock.getStockNum(), vo.getStockNum());
        BigDecimal costNum = vo.getStockNum();
        BigDecimal pendingNum = BigDecimal.ZERO;
        BigDecimal subTaxAmount = vo.getTaxAmount();

        if (subTaxAmount == null && supportPendingCost(vo)) {
            if (NumberUtil.le(productStock.getStockNum(), BigDecimal.ZERO)) {
                costNum = BigDecimal.ZERO;
                pendingNum = vo.getStockNum();
            } else if (NumberUtil.lt(productStock.getStockNum(), vo.getStockNum())) {
                costNum = productStock.getStockNum();
                pendingNum = NumberUtil.sub(vo.getStockNum(), productStock.getStockNum());
                subTaxAmount = productStock.getTaxAmount();
            } else {
                subTaxAmount = NumberUtil.equal(curStockNum, BigDecimal.ZERO) ? productStock.getTaxAmount()
                        : NumberUtil.mul(productStock.getTaxPrice(), vo.getStockNum());
            }
        } else if (subTaxAmount == null) {
            subTaxAmount = NumberUtil.equal(curStockNum, BigDecimal.ZERO) ? productStock.getTaxAmount()
                    : NumberUtil.mul(productStock.getTaxPrice(), vo.getStockNum());
        }

        if (subTaxAmount != null) {
            subTaxAmount = NumberUtil.getNumber(subTaxAmount, 2);
        }

        boolean reCalcCostPrice = subTaxAmount != null && NumberUtil.gt(curStockNum, BigDecimal.ZERO);

        int count = getBaseMapper().subStock(vo.getProductId(), vo.getScId(), vo.getStockNum(),
                subTaxAmount,
                productStock.getStockNum(), productStock.getTaxAmount(), reCalcCostPrice);
        if (count != 1) {
            throw new DefaultClientException(
                    "商品（" + product.getCode() + "）" + product.getName() + "出库失败，请稍后重试！");
        }

        AddLogWithSubStockVo addLogWithAddStockVo = new AddLogWithSubStockVo();
        addLogWithAddStockVo.setProductId(vo.getProductId());
        addLogWithAddStockVo.setScId(vo.getScId());
        addLogWithAddStockVo.setStockNum(vo.getStockNum());
        addLogWithAddStockVo.setTaxAmount(subTaxAmount);
        addLogWithAddStockVo.setOriStockNum(productStock.getStockNum());
        addLogWithAddStockVo.setCurStockNum(curStockNum);
        addLogWithAddStockVo.setOriTaxPrice(productStock.getTaxPrice());
        addLogWithAddStockVo.setCurTaxPrice(calcSubStockCurTaxPrice(reCalcCostPrice, curStockNum,
                productStock.getTaxPrice(), productStock.getTaxAmount(), subTaxAmount));
        addLogWithAddStockVo.setCreateTime(vo.getCreateTime());
        addLogWithAddStockVo.setBizId(vo.getBizId());
        addLogWithAddStockVo.setBizDetailId(vo.getBizDetailId());
        addLogWithAddStockVo.setBizCode(vo.getBizCode());
        addLogWithAddStockVo.setBizType(vo.getBizType());

        productStockLogService.addLogWithSubStock(addLogWithAddStockVo);

        ProductStockChangeDto stockChange = new ProductStockChangeDto();
        stockChange.setScId(vo.getScId());
        stockChange.setProductId(vo.getProductId());
        stockChange.setNum(vo.getStockNum());
        stockChange.setTaxAmount(subTaxAmount);
        stockChange.setCostNum(costNum);
        stockChange.setPendingNum(pendingNum);
        stockChange.setCurTaxPrice(addLogWithAddStockVo.getCurTaxPrice());
        stockChange.setCreateTime(vo.getCreateTime());
        stockChange.setCurStockNum(addLogWithAddStockVo.getCurStockNum());

        SubStockEvent subStockEvent = new SubStockEvent(this, stockChange);
        ApplicationUtil.publishEvent(subStockEvent);

        return stockChange;
    }

    /**
     * 生成产品库存
     * @param productId
     * @param scId
     * @return
     */
    private ProductStock generateProductStock(String productId, String scId) {
        ProductStock res = new ProductStock();
        res.setId(IdUtil.getId());
        res.setScId(scId);
        res.setProductId(productId);
        res.setStockNum(BigDecimal.ZERO);
        res.setTaxPrice(null);
        res.setTaxAmount(null);
        return res;
    }

    /**
     * 计算出库当前成本价
     * @param reCalcCostPrice
     * @param curStockNum
     * @param taxPrice
     * @param taxAmount
     * @param subTaxAmount
     * @return
     */
    private BigDecimal calcSubStockCurTaxPrice(boolean reCalcCostPrice,
                                               BigDecimal curStockNum,
                                               BigDecimal taxPrice,
                                               BigDecimal taxAmount,
                                               BigDecimal subTaxAmount) {

        if (!reCalcCostPrice) {
            return taxPrice;
        }

        return NumberUtil.equal(curStockNum, BigDecimal.ZERO)
                ? null
                : NumberUtil.getNumber(NumberUtil.div(NumberUtil.sub(taxAmount, subTaxAmount), curStockNum), 6);
    }

    private BigDecimal calcAddStockCurTaxPrice(boolean reCalcCostPrice,
                                               BigDecimal curStockNum,
                                               BigDecimal stockPrice,
                                               BigDecimal taxPrice,
                                               BigDecimal stockAmt,
                                               BigDecimal addTaxAmount) {

        if (!reCalcCostPrice) {
            return stockPrice;
        }

        return NumberUtil.equal(curStockNum, BigDecimal.ZERO)
                ? taxPrice
                : NumberUtil.getNumber(NumberUtil.div(NumberUtil.add(NumberUtil.getDefaultValue(stockAmt), addTaxAmount), curStockNum), 6);
    }

    /**
     * 查询产品库存
     * @param productId
     * @param scId
     * @return
     */
    private ProductStock getProductStock(String productId, String scId) {
        Wrapper<ProductStock> queryWrapper = Wrappers.lambdaQuery(ProductStock.class)
                .eq(ProductStock::getProductId, productId)
                .eq(ProductStock::getScId, scId);

        ProductStock productStock = getBaseMapper().selectOne(queryWrapper);
        return productStock;
    }

    /**
     * 判断是否支持挂单成本
     * 
     * @param vo
     * @return
     */
    private boolean supportPendingCost(SubProductStockVo vo) {

        return vo.getBizType() != null && (vo.getBizType().equals(ProductStockBizType.SALE.getCode())
                || vo.getBizType().equals(ProductStockBizType.RETAIL.getCode()));
    }
}
